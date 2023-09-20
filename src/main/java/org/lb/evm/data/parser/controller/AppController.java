package org.lb.evm.data.parser.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.osslabz.evm.abi.decoder.AbiDecoder;
import net.osslabz.evm.abi.decoder.DecodedFunctionCall;
import net.osslabz.evm.abi.definition.AbiDefinition;
import net.osslabz.evm.abi.definition.SolidityType;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.lb.evm.data.parser.code.EncodeMethdInfo;
import org.lb.evm.data.parser.dao.DecodeParam;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;

@Controller
public class AppController {
    private static DecodeParam DECODE_PARAM = new DecodeParam();

    @GetMapping("/start")
    public String showForm(Model model) {
        model.addAttribute("decodeParam", DECODE_PARAM);
        return "parser";
    }

    @GetMapping("/index")
    public String index(Model model) {
        List<String> list = Arrays.asList("Spring MVC Demo");
        model.addAttribute("list", list);
        return "index";
    }


    @PostMapping("/decode")
    public String decode(Model model, @ModelAttribute DecodeParam decodeParam) throws JsonProcessingException {
        String abi = decodeParam.getAbi();
        String inputData = decodeParam.getInputData();
        String methodInfo = decodeParam.getMethodInfo();
        System.out.println("abi: " + abi);
        System.out.println("inputData: " + inputData);
        System.out.println("methodInfo: " + methodInfo);
        if(StringUtils.isEmpty(abi)) {
            throw new RuntimeException("abi is empty");
        }
        if(StringUtils.isEmpty(inputData) && StringUtils.isEmpty(methodInfo)) {
            throw new RuntimeException("inputData and methodInfo are not empty at the same time");
        }
        InputStream inputStream = new ByteArrayInputStream(abi.getBytes(StandardCharsets.UTF_8));
        if(StringUtils.isNotEmpty(inputData)) {
            if(!inputData.startsWith("0x")) {
                throw new RuntimeException("inputData must start with 0x");
            }
            AbiDecoder abiEtherumDecoder = new AbiDecoder(inputStream);
            DecodedFunctionCall decodedFunctionCall = abiEtherumDecoder.decodeFunctionCall(inputData);

            StringBuilder strBuilder = new StringBuilder("{ \n" +
                    "\"methodName\":" + "\"" + decodedFunctionCall.getName()+"\",\n");
            strBuilder.append("\"methodParams\":{ \n");

            for(DecodedFunctionCall.Param param : decodedFunctionCall.getParams()) {
                strBuilder.append("\"" + param.getName() + "\":" + "\"" + param.getValue() + "\",\n");
            }
            strBuilder.deleteCharAt(strBuilder.length() - 2);
            strBuilder.append("}\n");
            strBuilder.append("}\n");
            methodInfo = strBuilder.toString();
            DECODE_PARAM.setAbi(abi);
            DECODE_PARAM.setInputData(inputData);
            DECODE_PARAM.setMethodInfo(methodInfo);
            decodeParam.setMethodInfo(methodInfo);
        }
        else {
            EncodeMethdInfo encodeMethodInputData = new EncodeMethdInfo(inputStream);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode actualObj = mapper.readTree(methodInfo);
            String methodName = actualObj.get("methodName").asText();
            JsonNode methodParams = actualObj.get("methodParams");

            AbiDefinition.Entry entry = encodeMethodInputData.getMethodEntry(methodName);
            Set<Object> paramValue = new LinkedHashSet<>();
            for(AbiDefinition.Entry.Param param : entry.inputs) {
                String name = param.getName();
                if(methodParams.get(name).isNumber()) {
                   paramValue.add(methodParams.get(name).numberValue());
                }else{ //byte32 特殊处理
                    if(param.getType() instanceof SolidityType.Bytes32Type) {
                        String tempParam = methodParams.get(name).asText();
                        if(tempParam.startsWith("0x")) {
                            String noPrefix = tempParam.substring(2);
                            byte[] bytes = Hex.decode(noPrefix);
                            paramValue.add(bytes);
                        } else {
                            byte[] bytes = Hex.decode(tempParam);
                            paramValue.add(bytes);
                        }
                    }else {
                        paramValue.add(methodParams.get(name).asText());
                    }
                }

            }
            String result = encodeMethodInputData.encodeMethod(methodName, paramValue.toArray());
            decodeParam.setInputData(result);
            DECODE_PARAM.setAbi(abi);
            DECODE_PARAM.setMethodInfo(methodInfo);
            DECODE_PARAM.setInputData(result);
        }

        return "redirect:start";
    }

    @PostMapping("/encode")
    public String encode(Model model, @ModelAttribute DecodeParam decodeParam) throws JsonProcessingException {
        String abi = decodeParam.getAbi();
        String methodInfo = decodeParam.getMethodInfo();
        System.out.println("abi: " + abi);

        System.out.println("methodInfo: " + methodInfo);
        if(StringUtils.isEmpty(abi)) {
            throw new RuntimeException("abi is required!");
        }
        if(StringUtils.isEmpty(methodInfo)) {
            throw new RuntimeException("methodInfo is required!");
        }
        InputStream inputStream = new ByteArrayInputStream(abi.getBytes(StandardCharsets.UTF_8));

        EncodeMethdInfo encodeMethodInputData = new EncodeMethdInfo(inputStream);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(methodInfo);
        String methodName = actualObj.get("methodName").asText();
        JsonNode methodParams = actualObj.get("methodParams");

        AbiDefinition.Entry entry = encodeMethodInputData.getMethodEntry(methodName);
        Set<Object> paramValue = new LinkedHashSet<>();
        for(AbiDefinition.Entry.Param param : entry.inputs) {
            String name = param.getName();
            if(methodParams.get(name).isNumber()) {
                paramValue.add(methodParams.get(name).numberValue());
            }else{ //byte32 特殊处理
                if(param.getType() instanceof SolidityType.Bytes32Type) {
                    String tempParam = methodParams.get(name).asText();
                    if(tempParam.startsWith("0x")) {
                        String noPrefix = tempParam.substring(2);
                        byte[] bytes = Hex.decode(noPrefix);
                        paramValue.add(bytes);
                    } else {
                        byte[] bytes = Hex.decode(tempParam);
                        paramValue.add(bytes);
                    }
                }else {
                    paramValue.add(methodParams.get(name).asText());
                }
            }

        }
        String result = encodeMethodInputData.encodeMethod(methodName, paramValue.toArray());
        decodeParam.setInputData(result);
        DECODE_PARAM.setAbi(abi);
        DECODE_PARAM.setMethodInfo(methodInfo);
        DECODE_PARAM.setInputData(result);


        return "redirect:start";
    }


}
