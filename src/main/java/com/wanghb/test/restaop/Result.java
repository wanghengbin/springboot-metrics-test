package com.wanghb.test.restaop;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result {

    public static final int SUCCCODE = 0;

    public static final int COMMONFAILCODE = 1;

    private int retCode;

    private String retMsg;

    private Object data;

    public boolean isSucc(){
        return retCode == 0;
    }

    public static Result ok(){
        return res(0, "成功", null);
    }

    public static Result ok(Object data){
        return res(0, "成功", data);
    }

    public static Result fail(){
        return res(1, "失败", null);
    }

    public static Result fail(String msg){
        return res(1, msg, null);
    }

    public static Result res(int retCode, String retMsg){
        return res(retCode, retMsg, null);
    }

    public static Result res(int retCode, String retMsg, Object data){
        return new Result(retCode, retMsg, data);
    }
}
