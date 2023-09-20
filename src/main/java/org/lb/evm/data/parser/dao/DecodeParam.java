package org.lb.evm.data.parser.dao;

import lombok.Data;

@Data
public class DecodeParam {
    private String abi;
    private String inputData;
    private String methodInfo;
}
