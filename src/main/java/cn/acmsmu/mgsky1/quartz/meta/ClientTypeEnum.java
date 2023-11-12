package cn.acmsmu.mgsky1.quartz.meta;

public enum ClientTypeEnum {
    STANDALONE("standalone", "单机模式"),
    DISTRIBUTE("distribute", "分布式模式");

    private String code;

    private String desc;

    ClientTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ClientTypeEnum getByCode(String code) {
        for (ClientTypeEnum item : ClientTypeEnum.values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }

    public Boolean equalByCode(String code) {
        ClientTypeEnum item = getByCode(code);
        if (item == null) {
            return false;
        }
        return item.getCode().equals(code);
    }
}
