package org.lb.evm.data.parser.code;

import net.osslabz.evm.abi.definition.AbiDefinition;
import org.bouncycastle.util.encoders.Hex;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class EncodeMethdInfo {
    private final AbiDefinition abi;
    private Map<String, AbiDefinition.Entry> methodEntryMap = new HashMap();
    private final static String HEX_PREFIX = "0x";

    public EncodeMethdInfo(InputStream inputStream) {
        this.abi = AbiDefinition.fromJson(inputStream);
        init();
    }

    public EncodeMethdInfo(String abiJson) {
        this.abi = AbiDefinition.fromJson(abiJson);
        init();
    }

    public AbiDefinition.Entry getMethodEntry(String methodName) {
        return this.methodEntryMap.get(methodName);
    }



    public String encodeMethod(String methodName, Object... args) {
        AbiDefinition.Entry entry = this.methodEntryMap.get(methodName);
        if (entry == null) {
            throw new RuntimeException("Method " + methodName + " not found");
        } else {
            AbiDefinition.Function function = (AbiDefinition.Function)entry;
            byte[] signature = function.encode(args);

            return HEX_PREFIX+ Hex.toHexString(signature);
        }
    }


    private void init() {
        Iterator var1 = this.abi.iterator();

        while(var1.hasNext()) {
            AbiDefinition.Entry entry = (AbiDefinition.Entry) var1.next();
            this.methodEntryMap.put(entry.name, entry);
        }
    }
}
