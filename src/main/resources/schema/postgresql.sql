-- Storage PostgreSQL schema

CREATE TABLE IF NOT EXISTS {prefix}PlayerData (
  player VARCHAR(36) NOT NULL,
  data TEXT,
  max BIGINT NOT NULL,
  autopickup BOOLEAN DEFAULT FALSE,
  PRIMARY KEY (player)
);

CREATE TABLE IF NOT EXISTS {prefix}TransferLogs (
  id BIGSERIAL PRIMARY KEY,
  sender VARCHAR(36) NOT NULL,
  receiver VARCHAR(36) NOT NULL,
  material VARCHAR(50) NOT NULL,
  amount INTEGER NOT NULL,
  timestamp BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_{prefix}transfer_sender ON {prefix}TransferLogs (sender);
CREATE INDEX IF NOT EXISTS idx_{prefix}transfer_receiver ON {prefix}TransferLogs (receiver);
CREATE INDEX IF NOT EXISTS idx_{prefix}transfer_timestamp ON {prefix}TransferLogs (timestamp);

CREATE TABLE IF NOT EXISTS {prefix}CropTransferLogs (
  id BIGSERIAL PRIMARY KEY,
  sender VARCHAR(36) NOT NULL,
  receiver VARCHAR(36) NOT NULL,
  item_name VARCHAR(50) NOT NULL,
  amount INTEGER NOT NULL,
  timestamp BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_{prefix}crop_transfer_sender ON {prefix}CropTransferLogs (sender);
CREATE INDEX IF NOT EXISTS idx_{prefix}crop_transfer_receiver ON {prefix}CropTransferLogs (receiver);
CREATE INDEX IF NOT EXISTS idx_{prefix}crop_transfer_timestamp ON {prefix}CropTransferLogs (timestamp);

CREATE TABLE IF NOT EXISTS {prefix}MobTransferLogs (
  id BIGSERIAL PRIMARY KEY,
  sender VARCHAR(36) NOT NULL,
  receiver VARCHAR(36) NOT NULL,
  item_name VARCHAR(50) NOT NULL,
  amount INTEGER NOT NULL,
  timestamp BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_{prefix}mob_transfer_sender ON {prefix}MobTransferLogs (sender);
CREATE INDEX IF NOT EXISTS idx_{prefix}mob_transfer_receiver ON {prefix}MobTransferLogs (receiver);
CREATE INDEX IF NOT EXISTS idx_{prefix}mob_transfer_timestamp ON {prefix}MobTransferLogs (timestamp);

CREATE TABLE IF NOT EXISTS {prefix}MythicTransferLogs (
  id BIGSERIAL PRIMARY KEY,
  sender VARCHAR(36) NOT NULL,
  receiver VARCHAR(36) NOT NULL,
  item_name VARCHAR(50) NOT NULL,
  amount INTEGER NOT NULL,
  timestamp BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_{prefix}mythic_transfer_sender ON {prefix}MythicTransferLogs (sender);
CREATE INDEX IF NOT EXISTS idx_{prefix}mythic_transfer_receiver ON {prefix}MythicTransferLogs (receiver);
CREATE INDEX IF NOT EXISTS idx_{prefix}mythic_transfer_timestamp ON {prefix}MythicTransferLogs (timestamp);

CREATE TABLE IF NOT EXISTS {prefix}StorageFriends (
  player_uuid VARCHAR(36) NOT NULL,
  friend_uuid VARCHAR(36) NOT NULL,
  created_at BIGINT NOT NULL,
  PRIMARY KEY (player_uuid, friend_uuid)
);

CREATE TABLE IF NOT EXISTS {prefix}StorageFriendRequests (
  sender_uuid VARCHAR(36) NOT NULL,
  receiver_uuid VARCHAR(36) NOT NULL,
  created_at BIGINT NOT NULL,
  expires_at BIGINT NOT NULL,
  PRIMARY KEY (sender_uuid, receiver_uuid)
);

CREATE TABLE IF NOT EXISTS {prefix}StorageFriendLogs (
  id BIGSERIAL PRIMARY KEY,
  owner_uuid VARCHAR(36) NOT NULL,
  actor_uuid VARCHAR(36) NOT NULL,
  action VARCHAR(32) NOT NULL,
  detail TEXT,
  timestamp BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS {prefix}StorageFriendAccessSettings (
  player_uuid VARCHAR(36) NOT NULL,
  storage_type VARCHAR(32) NOT NULL,
  allowed BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (player_uuid, storage_type)
);

CREATE TABLE IF NOT EXISTS {prefix}StorageFriendActionSettings (
  player_uuid VARCHAR(36) NOT NULL,
  storage_type VARCHAR(32) NOT NULL,
  action VARCHAR(32) NOT NULL,
  allowed BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (player_uuid, storage_type, action)
);

CREATE INDEX IF NOT EXISTS idx_{prefix}storage_friend_logs_owner_time ON {prefix}StorageFriendLogs (owner_uuid, timestamp);
CREATE INDEX IF NOT EXISTS idx_{prefix}storage_friend_logs_actor_time ON {prefix}StorageFriendLogs (actor_uuid, timestamp);