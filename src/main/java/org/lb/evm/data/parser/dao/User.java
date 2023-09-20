package org.lb.evm.data.parser.dao;

import lombok.Data;

import java.util.Date;

@Data
public class User {
    private String name;
    private String email;
    private String password;
    private String gender;
    private String note;
    private boolean married;
    private Date birthday;
    private String profession;
}
