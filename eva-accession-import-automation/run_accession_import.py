import os
import argparse
import json
import subprocess
from argparse import RawTextHelpFormatter
from __init__ import *


def run_command(command_description, command):
    logger.info("Starting process: " + command_description)
    with subprocess.Popen(command, stdout=subprocess.PIPE, bufsize=1, universal_newlines=True, shell=True) as process:
        for line in process.stdout:
            print(line, end='')
    if process.returncode != 0:
        logger.error(command_description + " failed!")
        raise subprocess.CalledProcessError(process.returncode, process.args)
    else:
        logger.info(command_description + " completed successfully")
    return process.returncode


def get_args_from_private_config_file(private_config_file):
    with open(private_config_file) as private_config_file_handle:
        return json.load(private_config_file_handle)


def get_commands_to_run(command_line_args):
    program_args = command_line_args.copy()
    program_args.update(get_args_from_private_config_file(command_line_args["private_config_file"]))

    program_args["scientific_name"] = program_args["scientific_name"].lower()
    program_args["program_dir"] = os.path.dirname(os.path.realpath(__file__)) + os.path.sep

    program_args["species_assembly_folder"] = os.path.sep.join(["{eva_root_dir}", "datasources",
                                                                "reference_sequences",
                                                                "{scientific_name}",
                                                                "{assembly_accession}"]).format(**program_args)
    program_args["species_assembly_report_folder"] = os.path.sep.join(["{eva_root_dir}", "datasources",
                                                                       "assembly_reports",
                                                                       "{scientific_name}",
                                                                       "{assembly_accession}"]).format(**program_args)
    program_args["species_accessioning_import_folder"] = os.path.sep.join(["{eva_root_dir}",
                                                                           "dbsnp-importer-accessioning",
                                                                           "{species}"]).format(**program_args)

    program_args["assembly_report"] = os.path.sep.join(["{species_assembly_report_folder}",
                                                        "{assembly_accession}_custom.txt"]).format(**program_args)
    program_args["fasta_file_path"] = os.path.sep.join(["{species_assembly_folder}",
                                                        "{assembly_accession}.fa"]).format(**program_args)

    create_species_assembly_folders_command = "mkdir -p {species_assembly_folder} && " \
                                              "mkdir -p {species_assembly_report_folder}".format(**program_args)
    create_species_accessioning_import_folder_command = "mkdir -p {species_accessioning_import_folder}" \
        .format(**program_args)

    generate_custom_assembly_report_command = "cd {species_assembly_report_folder} && " \
                                              "{python3_path} {program_dir}generate_custom_assembly_report.py " \
                                              "-d {metadb} -u {metauser} -h {metahost} " \
                                              "-s {species} -a {assembly_accession} " \
                                              "-g {genbank_equivalents_file}".format(**program_args)

    create_fasta_file_command = "bash {program_dir}create_fasta_from_assembly_report.sh {assembly_accession} " \
                                "{assembly_report} {species_assembly_folder}".format(**program_args)

    generate_import_job_properties_file_command = ("cd {species_accessioning_import_folder} && " +
                                                   "{python3_path} {program_dir}generate_properties.py " +
                                                   "-s {species} " +
                                                   "-b {build} " + ("-l " if program_args["latest_build"] else " ") +
                                                   "-n {assembly_name} -a {assembly_accession} -r {assembly_report} " +
                                                   "-f {fasta_file_path} " +
                                                   "-d {metadb} -u {metauser} -h {metahost} " +
                                                   "-D {job_tracker_db} -H {job_tracker_host} " +
                                                   "--mongo-acc-db {mongo_acc_db} --mongo-auth-db {mongo_auth_db} " +
                                                   "--mongo-user {mongo_user} --mongo-password {mongo_password} " +
                                                   "--mongo-host {mongo_host} --mongo-port {mongo_port}")\
                                                   .format(**program_args)

    program_args["properties_file_path"] = os.path.sep.join(["{species_accessioning_import_folder}",
                                                             "{assembly_accession}_b{build}.properties"]
                                                            ).format(**program_args)
    run_accession_import_command = "cd {species_accessioning_import_folder} && " \
                                   "java -jar -Xmx5g {accession_import_jar} " \
                                   "--spring.config.location={properties_file_path}".format(**program_args)

    ss_counts_validation_command = ("cd {validation_script_path} && " +
                                    "bash ss_counts.sh {assembly_accession} {assembly_name} {species} {build} " +
                                    os.path.sep.join(["{species_accessioning_import_folder}", "accessioning-qa",
                                                      "{env}"]) +
                                    " {env}").format(**program_args)
    rs_counts_validation_command = ss_counts_validation_command.replace("ss_counts", "rs_counts")

    return [
        ("Create assembly FASTA and report folders", create_species_assembly_folders_command),
        ("Create accessioning import folder", create_species_accessioning_import_folder_command),
        ("Generate custom assembly report", generate_custom_assembly_report_command),
        ("Create FASTA file", create_fasta_file_command),
        ("Generate properties file for accessioning import", generate_import_job_properties_file_command),
        ("Run accession import", run_accession_import_command),
        ("Validate SS counts", ss_counts_validation_command),
        ("Validate RS counts", rs_counts_validation_command)
        ]


def main(command_line_args):
    for command_description, command in get_commands_to_run(vars(command_line_args))[command_line_args.step-1:]:
        #print(command + os.linesep)
        run_command(command_description, command)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description='Run the accession import process for a given species, assembly and build', add_help=False,
        formatter_class=RawTextHelpFormatter)
    parser.add_argument("-s", "--species", help="Species for which the process has to be run", required=True)
    parser.add_argument("--scientific-name", help="Scientific name for the species", required=True)
    parser.add_argument("-a", "--assembly-accession", help="Assembly for which the process has to be run",
                        required=True)
    parser.add_argument("-b", "--build", help="dbSNP build number, e.g. 151", required=True)
    parser.add_argument("-l", "--latest-build",
                        help="Flag that this build is the latest (relevant for dbsnp table name)",
                        action='store_true')
    parser.add_argument("-n", "--assembly-name",
                        help="Assembly name for which the process has to be run, e.g. Gallus_gallus-5.0"
                             ". (Can be ommited if there is only one assembly name in the build)")
    parser.add_argument("-p", "--private-config-file",
                        help="Path to the configuration file with private connection details, credentials etc.,")
    parser.add_argument("--step", help= os.linesep + "Run from a specific step number." + os.linesep +
                                       "1. Create assembly FASTA and report folders" + os.linesep +
                                       "2. Create accessioning import folder" + os.linesep +
                                       "3. Generate custom assembly report" + os.linesep +
                                       "4. Create FASTA file" + os.linesep +
                                       "5. Generate properties file for accessioning import" + os.linesep +
                                       "6. Run accession import" + os.linesep +
                                       "7. Validate SS counts" + os.linesep +
                                       "8. Validate RS counts",
                        default=1, type=int)
    parser.add_argument('--help', action='help', help='Show this help message and exit')

    args = {}
    try:
        args = parser.parse_args()
        if args.step > 8:
            logger.error("Use only the step numbers that appear in the help message!")
            sys.exit(1)
        main(args)
    except Exception as ex:
        logger.exception(ex)
        sys.exit(1)

    sys.exit(0)
