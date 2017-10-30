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

package com.emc.metalnx.services.machine;

import com.emc.metalnx.services.interfaces.MachineInfoService;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Service
public class MachineInfoServiceImpl implements MachineInfoService {

	@Override
	public String getAddress(String hostName) throws UnknownHostException {
		
		InetAddress inetAddress = InetAddress.getByName(hostName);
		
		return inetAddress.getHostAddress();
	}	

	@Override
	public String getHostName(String ip) throws UnknownHostException {
		InetAddress inetAddress = InetAddress.getByName(ip);
		
		return inetAddress.getHostName();
	}

}
