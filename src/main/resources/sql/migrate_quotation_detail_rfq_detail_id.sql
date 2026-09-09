-- Backfill quotation_detail.rfq_detail_id from the RFQ tier referenced by tier_id.
-- Run this once on MySQL after taking a database backup.

ALTER TABLE quotation_detail
    ADD COLUMN rfq_detail_id BIGINT NULL AFTER tier_id;

CREATE INDEX idx_quotation_detail_rfq_detail_id
    ON quotation_detail (rfq_detail_id);

-- Only rows with a numeric tier_id are updated. This prevents an invalid legacy value
-- from being cast to 0 and accidentally linked to an unrelated RFQ detail.
UPDATE quotation_detail qd
JOIN rfq_tier rt ON rt.id = CAST(qd.tier_id AS UNSIGNED)
SET qd.rfq_detail_id = rt.rfq_detail_id
WHERE qd.rfq_detail_id IS NULL
  AND qd.tier_id REGEXP '^[0-9]+$';

-- Review any remaining rows. They are normally manually created quotation lines,
-- legacy lines with no tier_id, or lines whose referenced tier no longer exists.
SELECT
    qd.id,
    qd.quotation_no,
    qd.line_no,
    qd.name,
    qd.tier_id,
    qd.source_rfq_id
FROM quotation_detail qd
WHERE qd.rfq_detail_id IS NULL
ORDER BY qd.quotation_no, qd.line_no;
