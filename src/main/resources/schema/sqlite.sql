-- Storage SQLite schema

CREATE TABLE IF NOT EXISTS {prefix}PlayerData (
  player VARCHAR(36) NOT NULL,
  data TEXT DEFAULT '{}',
  max BIGINT NOT NULL,
  autopickup BOOLEAN DEFAULT 0,
  PRIMARY KEY (player)
);

CREATE TABLE IF NOT EXISTS {prefix}TransferLogs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  sender TEXT NOT NULL,
  receiver TEXT NOT NULL,
  material TEXT NOT NULL,
  amount INTEGER NOT NULL,
  timestamp LONG NOT NULL,
  status TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS {prefix}CropTransferLogs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  sender TEXT NOT NULL,
  receiver TEXT NOT NULL,
  item_name TEXT NOT NULL,
  amount INTEGER NOT NULL,
  timestamp LONG NOT NULL,
  status TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS {prefix}MobTransferLogs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  sender TEXT NOT NULL,
  receiver TEXT NOT NULL,
  item_name TEXT NOT NULL,
  amount INTEGER NOT NULL,
  timestamp LONG NOT NULL,
  status TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS {prefix}MythicTransferLogs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  sender TEXT NOT NULL,
  receiver TEXT NOT NULL,
  item_name TEXT NOT NULL,
  amount INTEGER NOT NULL,
  timestamp LONG NOT NULL,
  status TEXT NOT NULL
);

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
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  owner_uuid VARCHAR(36) NOT NULL,
  actor_uuid VARCHAR(36) NOT NULL,
  action VARCHAR(32) NOT NULL,
  detail TEXT,
  timestamp BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS {prefix}StorageFriendAccessSettings (
  player_uuid VARCHAR(36) NOT NULL,
  storage_type VARCHAR(32) NOT NULL,
  allowed BOOLEAN NOT NULL DEFAULT 0,
  PRIMARY KEY (player_uuid, storage_type)
);

CREATE TABLE IF NOT EXISTS {prefix}StorageFriendActionSettings (
  player_uuid VARCHAR(36) NOT NULL,
  storage_type VARCHAR(32) NOT NULL,
  action VARCHAR(32) NOT NULL,
  allowed BOOLEAN NOT NULL DEFAULT 0,
  PRIMARY KEY (player_uuid, storage_type, action)
);

CREATE INDEX IF NOT EXISTS idx_{prefix}storage_friend_logs_owner_time ON {prefix}StorageFriendLogs (owner_uuid, timestamp);
CREATE INDEX IF NOT EXISTS idx_{prefix}storage_friend_logs_actor_time ON {prefix}StorageFriendLogs (actor_uuid, timestamp);