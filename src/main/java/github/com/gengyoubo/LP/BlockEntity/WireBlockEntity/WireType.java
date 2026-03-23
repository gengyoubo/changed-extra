package github.com.gengyoubo.LP.BlockEntity.WireBlockEntity;

public enum WireType {
    BASIC(100, 1000);


    // 每tick最大传输量
    public final int maxTransfer;

    // 内部缓存容量（导线能存多少电）
    public final int capacity;

    WireType(int maxTransfer, int capacity) {
        this.maxTransfer = maxTransfer;
        this.capacity = capacity;
    }
}
