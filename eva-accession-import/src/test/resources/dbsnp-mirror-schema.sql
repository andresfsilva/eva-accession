CREATE TABLE dbsnp_variant_load_nohgvslink_d8c757988871529f37061fa9c79477a5 (
	batch_id integer NULL,
	batch_name varchar(64) NULL,
	batch_handle varchar(20) NULL,
	tax_id integer NULL,
	univar_id integer NULL,
	var_str varchar(1024) NULL,
	subsnp_class smallint NULL,
	rs_id bigint NULL,
	ss_id bigint NULL,
	contig_name varchar(32) NULL,
	contig_start integer NULL,
	chromosome varchar(32) NULL,
	chromosome_start integer NULL,
	reference varchar(1024) NULL,
	alleles varchar(1024) NULL,
	loc_type smallint NULL,
	subsnp_orientation integer NULL,
	snp_orientation integer NULL,
	contig_orientation integer NULL,
	asn_from integer NULL,
	phys_pos_from integer NULL,
	lc_ngbr integer NULL,
	genotype_exists integer NULL,
	freq_exists integer NULL,
	ss_create_time timestamp without time zone NULL,
	rs_create_time timestamp without time zone NULL,
	load_order serial NOT NULL
);