-- Storage MariaDB schema

CREATE TABLE IF NOT EXISTS {prefix}PlayerData (
  player VARCHAR(36) NOT NULL,
  data TEXT,
  max BIGINT NOT NULL,
  autopickup BOOLEAN DEFAULT FALSE,
  PRIMARY KEY (player)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS {prefix}TransferLogs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  sender VARCHAR(36) NOT NULL,
  receiver VARCHAR(36) NOT NULL,
  material VARCHAR(50) NOT NULL,
  amount INTEGER NOT NULL,
  timestamp BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_{prefix}transfer_sender (sender),
  INDEX idx_{prefix}transfer_receiver (receiver),
  INDEX idx_{prefix}transfer_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS {prefix}CropTransferLogs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  sender VARCHAR(36) NOT NULL,
  receiver VARCHAR(36) NOT NULL,
  item_name VARCHAR(50) NOT NULL,
  amount INTEGER NOT NULL,
  timestamp BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_{prefix}crop_transfer_sender (sender),
  INDEX idx_{prefix}crop_transfer_receiver (receiver),
  INDEX idx_{prefix}crop_transfer_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS {prefix}MobTransferLogs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  sender VARCHAR(36) NOT NULL,
  receiver VARCHAR(36) NOT NULL,
  item_name VARCHAR(50) NOT NULL,
  amount INTEGER NOT NULL,
  timestamp BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_{prefix}mob_transfer_sender (sender),
  INDEX idx_{prefix}mob_transfer_receiver (receiver),
  INDEX idx_{prefix}mob_transfer_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS {prefix}MythicTransferLogs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  sender VARCHAR(36) NOT NULL,
  receiver VARCHAR(36) NOT NULL,
  item_name VARCHAR(50) NOT NULL,
  amount INTEGER NOT NULL,
  timestamp BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_{prefix}mythic_transfer_sender (sender),
  INDEX idx_{prefix}mythic_transfer_receiver (receiver),
  INDEX idx_{prefix}mythic_transfer_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS {prefix}StorageFriends (
  player_uuid VARCHAR(36) NOT NULL,
  friend_uuid VARCHAR(36) NOT NULL,
  created_at BIGINT NOT NULL,
  PRIMARY KEY (player_uuid, friend_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS {prefix}StorageFriendRequests (
  sender_uuid VARCHAR(36) NOT NULL,
  receiver_uuid VARCHAR(36) NOT NULL,
  created_at BIGINT NOT NULL,
  expires_at BIGINT NOT NULL,
  PRIMARY KEY (sender_uuid, receiver_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS {prefix}StorageFriendLogs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  owner_uuid VARCHAR(36) NOT NULL,
  actor_uuid VARCHAR(36) NOT NULL,
  action VARCHAR(32) NOT NULL,
  detail TEXT,
  timestamp BIGINT NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS {prefix}StorageFriendAccessSettings (
  player_uuid VARCHAR(36) NOT NULL,
  storage_type VARCHAR(32) NOT NULL,
  allowed BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (player_uuid, storage_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS {prefix}StorageFriendActionSettings (
  player_uuid VARCHAR(36) NOT NULL,
  storage_type VARCHAR(32) NOT NULL,
  action VARCHAR(32) NOT NULL,
  allowed BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (player_uuid, storage_type, action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_{prefix}storage_friend_logs_owner_time ON {prefix}StorageFriendLogs (owner_uuid, timestamp);
CREATE INDEX idx_{prefix}storage_friend_logs_actor_time ON {prefix}StorageFriendLogs (actor_uuid, timestamp);