package github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity;

public enum WireType {
    BASIC();


    // 每tick最大传输量
    public final int maxTransfer;

    // 内部缓存容量（导线能存多少电）
    public final int capacity;

    WireType() {
        this.maxTransfer = 100;
        this.capacity = 1000;
    }
}
