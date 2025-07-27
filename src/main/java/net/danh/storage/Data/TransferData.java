package net.danh.storage.Data;

public class TransferData {
    private int id;
    private String sender;
    private String receiver;
    private String material;
    private int amount;
    private long timestamp;
    private String status;

    public TransferData(String sender, String receiver, String material, int amount, long timestamp, String status) {
        this.sender = sender;
        this.receiver = receiver;
        this.material = material;
        this.amount = amount;
        this.timestamp = timestamp;
        this.status = status;
    }

    public TransferData(int id, String sender, String receiver, String material, int amount, long timestamp, String status) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.material = material;
        this.amount = amount;
        this.timestamp = timestamp;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
