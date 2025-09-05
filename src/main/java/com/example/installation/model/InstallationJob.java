package com.example.installation.model;

import java.util.ArrayList;
import java.util.List;

public class InstallationJob {
	private String jobId, status, priority, customerName, customerPhone, address, start, end, slaDue;
	private List<BomItem> bom = new ArrayList<>();
	private List<String> assigned = new ArrayList<>();

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String v) {
		this.jobId = v;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String v) {
		this.status = v;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String v) {
		this.priority = v;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String v) {
		this.customerName = v;
	}

	public String getCustomerPhone() {
		return customerPhone;
	}

	public void setCustomerPhone(String v) {
		this.customerPhone = v;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String v) {
		this.address = v;
	}

	public String getStart() {
		return start;
	}

	public void setStart(String v) {
		this.start = v;
	}

	public String getEnd() {
		return end;
	}

	public void setEnd(String v) {
		this.end = v;
	}

	public String getSlaDue() {
		return slaDue;
	}

	public void setSlaDue(String v) {
		this.slaDue = v;
	}

	public List<BomItem> getBom() {
		return bom;
	}

	public void setBom(List<BomItem> v) {
		this.bom = v;
	}

	public List<String> getAssigned() {
		return assigned;
	}

	public void setAssigned(List<String> v) {
		this.assigned = v;
	}
}
