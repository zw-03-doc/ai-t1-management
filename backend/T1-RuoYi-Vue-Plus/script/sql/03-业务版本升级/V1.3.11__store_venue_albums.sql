-- Preserve the previous brand album for rollback and bind it once to its original store.
CREATE TABLE IF NOT EXISTS fit_store_venue_gallery (
    tenant_id VARCHAR(20) NOT NULL, store_id BIGINT NOT NULL, version INT NOT NULL,
    legacy_owner TINYINT NULL,
    PRIMARY KEY (tenant_id,store_id), UNIQUE KEY uk_venue_legacy_owner (tenant_id,legacy_owner)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS fit_store_venue_photo (
    tenant_id VARCHAR(20) NOT NULL, store_id BIGINT NOT NULL, photo_id VARCHAR(36) NOT NULL,
    sort_order INT NOT NULL, image_jpeg MEDIUMBLOB NULL,
    PRIMARY KEY (tenant_id,store_id,photo_id), KEY idx_venue_store_order (tenant_id,store_id,sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TEMPORARY TABLE IF EXISTS venue_album_owners;
CREATE TEMPORARY TABLE venue_album_owners (tenant_id VARCHAR(20) PRIMARY KEY, store_id BIGINT NOT NULL, version INT NOT NULL);
START TRANSACTION;
-- An installation which already has multiple stores must identify the original owner explicitly.
INSERT INTO venue_album_owners
SELECT g.tenant_id, IF(COUNT(*)=1, MIN(s.store_id), MAX(IF(s.store_id=@venue_legacy_store_id,s.store_id,NULL))), g.version
FROM fit_venue_gallery g JOIN fit_store s ON s.tenant_id=g.tenant_id AND s.del_flag='0'
WHERE NOT EXISTS (SELECT 1 FROM fit_store_venue_gallery current_album
                  WHERE current_album.tenant_id=g.tenant_id AND current_album.legacy_owner=1)
GROUP BY g.tenant_id,g.version;
INSERT INTO fit_store_venue_gallery (tenant_id,store_id,version,legacy_owner)
SELECT tenant_id,store_id,version,1 FROM venue_album_owners;
INSERT INTO fit_store_venue_photo (tenant_id,store_id,photo_id,sort_order,image_jpeg)
SELECT owner.tenant_id,owner.store_id,p.photo_id,p.sort_order,p.image_jpeg
FROM venue_album_owners owner JOIN fit_venue_photo p ON p.tenant_id=owner.tenant_id;
COMMIT;
DROP TEMPORARY TABLE venue_album_owners;
