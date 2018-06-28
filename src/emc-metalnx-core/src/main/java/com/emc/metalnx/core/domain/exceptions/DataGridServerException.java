 /* Copyright (c) 2018, University of North Carolina at Chapel Hill */
 /* Copyright (c) 2015-2017, Dell EMC */
 

package com.emc.metalnx.core.domain.exceptions;

import org.springframework.security.core.AuthenticationException;

public class DataGridServerException extends AuthenticationException {
    public DataGridServerException(String message) {
        super(message);
    }
}
