package com.uob.meniga.model;

public class Data {

	private String outputValue;
	
	public String getOutputValue() {
		return outputValue;
	}
	
	public void setOutputValue(String outputValue) {
		this.outputValue = outputValue;
	}
	
	public void setHeader(String header) {
		this.setOutputValue(header+"\n"+this.getOutputValue());
	}
	
}
