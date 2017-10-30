/*
 * Copyright (c) 2015-2017, Dell EMC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.emc.metalnx.services.irods;

import com.emc.metalnx.core.domain.exceptions.DataGridConnectionRefusedException;
import com.emc.metalnx.services.interfaces.IRODSServices;
import com.emc.metalnx.services.interfaces.RemoteExecutionService;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.RemoteExecutionOfCommandsAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class RemoteExecutionServiceImpl implements RemoteExecutionService {

	@Autowired
	IRODSServices irodsServices;

	private static final int bufferSize = 8192;
	private static final String defaultEncoding = "UTF-8";

	//private static final Logger logger = LoggerFactory.getLogger(RemoteExecutionServiceImpl.class);
	
	@Override
	public String execute(String command) throws JargonException, IOException, 
		DataGridConnectionRefusedException {
		
			
		byte[] buffer = new byte[bufferSize];
		RemoteExecutionOfCommandsAO commandsAO = irodsServices.getRemoteExecutionOfCommandsAO();
		InputStream stream = commandsAO.executeARemoteCommandAndGetStreamGivingCommandNameAndArgs(command, "");
		
		StringBuilder resultBuilder = new StringBuilder();
		
		while(stream.read(buffer) != -1) {
			resultBuilder.append(new String(buffer, defaultEncoding));
		}
		
		stream.close();
		
		return resultBuilder.toString();
	}
}
