-- Copy every rfq_* table from the production schema to the test schema.
-- Prerequisite: `nutalig` and `nutalig_test` are schemas on the same MySQL server,
-- and the executing account has SELECT on `nutalig` plus INSERT on `nutalig_test`.
--
-- Copy only RFQs whose ID is greater than this value.  Set it to NULL to copy all RFQs.
SET @rfq_id_after = 'NTL-RFQ202609000044';

-- This is intentionally insert-only. Rows whose primary/unique key already exists
-- in nutalig_test are skipped; existing test data is never updated or deleted.
-- Attachments are not copied because RFQ picture rows only contain their URLs.

SET @previous_foreign_key_checks = @@FOREIGN_KEY_CHECKS;
SET @previous_group_concat_max_len = @@SESSION.group_concat_max_len;
SET FOREIGN_KEY_CHECKS = 0;
SET SESSION group_concat_max_len = 1048576;

DROP TEMPORARY TABLE IF EXISTS tmp_source_rfq_ids;
CREATE TEMPORARY TABLE tmp_source_rfq_ids (
    id VARCHAR(255) NOT NULL PRIMARY KEY
)
SELECT id
FROM nutalig.rfq_header
WHERE @rfq_id_after IS NULL OR BINARY id > BINARY @rfq_id_after;

-- A quotation is included when it directly references a selected RFQ or when one
-- of its rows in quotation_rfq_reference references a selected RFQ.
DROP TEMPORARY TABLE IF EXISTS tmp_source_quotation_nos;
CREATE TEMPORARY TABLE tmp_source_quotation_nos (
    quotation_no VARCHAR(50) NOT NULL PRIMARY KEY
)
SELECT DISTINCT quotation_row.quotation_no
FROM nutalig.quotations quotation_row
LEFT JOIN nutalig.quotation_rfq_reference quotation_reference
    ON BINARY quotation_reference.quotation_no = BINARY quotation_row.quotation_no
LEFT JOIN tmp_source_rfq_ids selected_reference_rfq
    ON BINARY selected_reference_rfq.id = BINARY quotation_reference.rfq_id
WHERE BINARY quotation_row.rfq_id IN (SELECT BINARY id FROM tmp_source_rfq_ids)
    OR BINARY quotation_row.reference_rfq_id IN (SELECT BINARY id FROM tmp_source_rfq_ids)
    OR selected_reference_rfq.id IS NOT NULL;

DELIMITER $$

DROP PROCEDURE IF EXISTS copy_rfq_tables_from_nutalig_to_test$$

CREATE PROCEDURE copy_rfq_tables_from_nutalig_to_test()
BEGIN
    DECLARE finished BOOLEAN DEFAULT FALSE;
    DECLARE current_table VARCHAR(64);
    DECLARE target_columns LONGTEXT;
    DECLARE filter_clause LONGTEXT;

    DECLARE rfq_table_cursor CURSOR FOR
        SELECT source_tables.table_name
        FROM information_schema.tables source_tables
        INNER JOIN information_schema.tables target_tables
            ON target_tables.table_schema = 'nutalig_test'
            AND target_tables.table_name = source_tables.table_name
            AND target_tables.table_type = 'BASE TABLE'
        WHERE source_tables.table_schema = 'nutalig'
            AND source_tables.table_type = 'BASE TABLE'
            AND (
                source_tables.table_name LIKE 'rfq!_%' ESCAPE '!'
                OR source_tables.table_name IN ('quotations', 'quotation_detail', 'quotation_rfq_reference')
            )
        ORDER BY
            CASE source_tables.table_name
                WHEN 'rfq_header' THEN 0
                WHEN 'quotations' THEN 1
                WHEN 'quotation_detail' THEN 2
                WHEN 'quotation_rfq_reference' THEN 2
                ELSE 3
            END,
            source_tables.table_name;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished = TRUE;

    OPEN rfq_table_cursor;

    copy_loop: LOOP
        FETCH rfq_table_cursor INTO current_table;
        IF finished THEN
            LEAVE copy_loop;
        END IF;

        SELECT GROUP_CONCAT(
                   CONCAT('`', REPLACE(column_name, '`', '``'), '`')
                   ORDER BY ordinal_position
                   SEPARATOR ', '
               )
        INTO target_columns
        FROM information_schema.columns
        WHERE table_schema = 'nutalig_test'
            AND table_name = current_table;

        IF current_table = 'quotations' THEN
            SET filter_clause = ' WHERE BINARY source_row.`quotation_no` IN (SELECT BINARY quotation_no FROM tmp_source_quotation_nos)';
        ELSEIF current_table IN ('quotation_detail', 'quotation_rfq_reference') THEN
            SET filter_clause = ' WHERE BINARY source_row.`quotation_no` IN (SELECT BINARY quotation_no FROM tmp_source_quotation_nos)';
        ELSEIF current_table = 'rfq_header' THEN
            SET filter_clause = ' WHERE BINARY source_row.`id` IN (SELECT BINARY id FROM tmp_source_rfq_ids)';
        ELSEIF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'nutalig' AND table_name = current_table AND column_name = 'rfq_header_id'
        ) THEN
            SET filter_clause = ' WHERE BINARY source_row.`rfq_header_id` IN (SELECT BINARY id FROM tmp_source_rfq_ids)';
        ELSEIF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'nutalig' AND table_name = current_table AND column_name IN ('rfq_id', 'source_rfq_id')
        ) THEN
            SELECT IF(
                EXISTS (
                    SELECT 1 FROM information_schema.columns
                    WHERE table_schema = 'nutalig' AND table_name = current_table AND column_name = 'rfq_id'
                ),
                ' WHERE BINARY source_row.`rfq_id` IN (SELECT BINARY id FROM tmp_source_rfq_ids)',
                ' WHERE BINARY source_row.`source_rfq_id` IN (SELECT BINARY id FROM tmp_source_rfq_ids)'
            ) INTO filter_clause;
        ELSEIF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'nutalig' AND table_name = current_table AND column_name = 'rfq_detail_id'
        ) THEN
            SET filter_clause = ' WHERE EXISTS (SELECT 1 FROM `nutalig`.`rfq_detail` detail_row '
                'INNER JOIN tmp_source_rfq_ids selected_rfq ON BINARY selected_rfq.id = BINARY detail_row.rfq_header_id '
                'WHERE detail_row.id = source_row.`rfq_detail_id`)';
        ELSEIF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'nutalig' AND table_name = current_table AND column_name = 'quote_id'
        ) THEN
            SET filter_clause = ' WHERE EXISTS (SELECT 1 FROM `nutalig`.`rfq_supplier_quote` quote_row '
                'INNER JOIN tmp_source_rfq_ids selected_rfq ON BINARY selected_rfq.id = BINARY quote_row.rfq_header_id '
                'WHERE BINARY quote_row.id = BINARY source_row.`quote_id`)';
        ELSEIF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'nutalig' AND table_name = current_table AND column_name = 'supplier_quote_id'
        ) THEN
            SET filter_clause = ' WHERE EXISTS (SELECT 1 FROM `nutalig`.`rfq_supplier_quote` quote_row '
                'INNER JOIN tmp_source_rfq_ids selected_rfq ON BINARY selected_rfq.id = BINARY quote_row.rfq_header_id '
                'WHERE BINARY quote_row.id = BINARY source_row.`supplier_quote_id`)';
        ELSEIF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'nutalig' AND table_name = current_table AND column_name = 'quote_detail_id'
        ) THEN
            SET filter_clause = ' WHERE EXISTS (SELECT 1 FROM `nutalig`.`rfq_supplier_quote_detail` quote_detail_row '
                'INNER JOIN `nutalig`.`rfq_supplier_quote` quote_row ON BINARY quote_row.id = BINARY quote_detail_row.quote_id '
                'INNER JOIN tmp_source_rfq_ids selected_rfq ON BINARY selected_rfq.id = BINARY quote_row.rfq_header_id '
                'WHERE quote_detail_row.id = source_row.`quote_detail_id`)';
        ELSE
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Cannot determine the RFQ relationship for an rfq_* table.';
        END IF;

        SET @copy_rfq_sql = CONCAT(
            'INSERT IGNORE INTO `nutalig_test`.`', REPLACE(current_table, '`', '``'), '` (', target_columns, ') ',
            'SELECT ', target_columns, ' FROM `nutalig`.`', REPLACE(current_table, '`', '``'), '` source_row', filter_clause
        );

        PREPARE copy_rfq_statement FROM @copy_rfq_sql;
        EXECUTE copy_rfq_statement;
        DEALLOCATE PREPARE copy_rfq_statement;
    END LOOP;

    CLOSE rfq_table_cursor;
END$$

CALL copy_rfq_tables_from_nutalig_to_test()$$
DROP PROCEDURE copy_rfq_tables_from_nutalig_to_test$$

DELIMITER ;

SET FOREIGN_KEY_CHECKS = @previous_foreign_key_checks;
SET SESSION group_concat_max_len = @previous_group_concat_max_len;
DROP TEMPORARY TABLE IF EXISTS tmp_source_rfq_ids;
DROP TEMPORARY TABLE IF EXISTS tmp_source_quotation_nos;

-- Review all tables included by the copy.
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'nutalig_test'
    AND table_type = 'BASE TABLE'
    AND (
        table_name LIKE 'rfq!_%' ESCAPE '!'
        OR table_name IN ('quotations', 'quotation_detail', 'quotation_rfq_reference')
    )
ORDER BY table_name;
