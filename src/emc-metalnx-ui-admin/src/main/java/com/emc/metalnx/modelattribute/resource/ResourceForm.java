 /* Copyright (c) 2018, University of North Carolina at Chapel Hill */
 /* Copyright (c) 2015-2017, Dell EMC */
 


package com.emc.metalnx.modelattribute.resource;

import com.emc.metalnx.core.domain.entity.DataGridResource;

import java.util.Date;
import java.util.List;

public class ResourceForm {

	//resource id in the data grid
	private long id;
	
	//resource name ("demoResc")
	private String name;
	
	//resource zone name
	private String zone;
	
	//resource type ("unix file system", "replication", etc)
	private String type;
	
	//resource path
	private String path;
	
	//resource free space
	private long freeSpace;
	
	//when the free space was calculated
	private Date freeSpaceTimeStamp;
	
	//other resources existing inside this resource
	private List<String> children;
	
	//resource parent name
	private String parent;
	
	//resource status ("up", "down")
	private String status;
	
	//resource host name
	private String host;
	
	//when the resource was created
	private Date createTime;
	
	//last time the resource was modified
	private Date modifyTime;
	
	//any information related to this resource
	private String info;
	
	//number of records existing in the resource
	private int totalRecords;
	
	//comment about a resource
	private String comment;
	
	//isilon parameters
	private String isiHost;
	private String isiPort;
	private String isiUser;
	
	public ResourceForm() {
		//empty constructor
	}
	
	public ResourceForm(DataGridResource dataGridResource) {
		this.id = dataGridResource.getId();
		this.name = dataGridResource.getName();
		this.createTime = dataGridResource.getCreateTime();
		this.freeSpace = dataGridResource.getFreeSpace();
		this.host = dataGridResource.getHost();
		this.parent = dataGridResource.getParent();
		this.path = dataGridResource.getPath();
		this.status = dataGridResource.getStatus();
		this.type = dataGridResource.getType();
		this.zone = dataGridResource.getZone();
		this.totalRecords = dataGridResource.getTotalRecords();
		this.children = dataGridResource.getChildren();
		this.modifyTime = dataGridResource.getModifyTime();
		this.freeSpaceTimeStamp = dataGridResource.getFreeSpaceDate();
		this.comment = dataGridResource.getComment();
		dataGridResource.getContextString();
	}

	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the zone
	 */
	public String getZone() {
		return zone;
	}

	/**
	 * @param zone the zone to set
	 */
	public void setZone(String zone) {
		this.zone = zone;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * @return the freeSpace
	 */
	public long getFreeSpace() {
		return freeSpace;
	}

	/**
	 * @param freeSpace the freeSpace to set
	 */
	public void setFreeSpace(long freeSpace) {
		this.freeSpace = freeSpace;
	}

	/**
	 * @return the freeSpaceTimeStamp
	 */
	public Date getFreeSpaceTimeStamp() {
		return freeSpaceTimeStamp;
	}

	/**
	 * @param freeSpaceTimeStamp the freeSpaceTimeStamp to set
	 */
	public void setFreeSpaceTimeStamp(Date freeSpaceTimeStamp) {
		this.freeSpaceTimeStamp = freeSpaceTimeStamp;
	}

	/**
	 * @return the children
	 */
	public List<String> getChildren() {
		return children;
	}

	/**
	 * @param children the children to set
	 */
	public void setChildren(List<String> children) {
		this.children = children;
	}

	/**
	 * @return the parent
	 */
	public String getParent() {
		return parent;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent(String parent) {
		this.parent = parent;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @param host the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * @return the createTime
	 */
	public Date getCreateTime() {
		return createTime;
	}

	/**
	 * @param createTime the createTime to set
	 */
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	/**
	 * @return the modifyTime
	 */
	public Date getModifyTime() {
		return modifyTime;
	}

	/**
	 * @param modifyTime the modifyTime to set
	 */
	public void setModifyTime(Date modifyTime) {
		this.modifyTime = modifyTime;
	}

	/**
	 * @return the info
	 */
	public String getInfo() {
		return info;
	}

	/**
	 * @param info the info to set
	 */
	public void setInfo(String info) {
		this.info = info;
	}

	/**
	 * @return the totalRecords
	 */
	public int getTotalRecords() {
		return totalRecords;
	}

	/**
	 * @param totalRecords the totalRecords to set
	 */
	public void setTotalRecords(int totalRecords) {
		this.totalRecords = totalRecords;
	}

	/**
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param comment the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * @return the contextString
	 */
	public String getContextString() {
		return "isi_host=" + this.isiHost + ";isi_port=" + this.isiPort + ";isi_user=" + this.isiUser;
	}

	/**
	 * @return the isiHost
	 */
	public String getIsiHost() {
		return isiHost;
	}

	/**
	 * @param isiHost the isiHost to set
	 */
	public void setIsiHost(String isiHost) {
		this.isiHost = isiHost;
	}

	/**
	 * @return the isiPort
	 */
	public String getIsiPort() {
		return isiPort;
	}

	/**
	 * @param isiPort the isiPort to set
	 */
	public void setIsiPort(String isiPort) {
		this.isiPort = isiPort;
	}

	/**
	 * @return the isiUser
	 */
	public String getIsiUser() {
		return isiUser;
	}

	/**
	 * @param isiUser the isiUser to set
	 */
	public void setIsiUser(String isiUser) {
		this.isiUser = isiUser;
	}
	
	
}
