/*
 * This file is part of MyDMAM.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.assetsxcross;

import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import hd3gtv.configuration.Configuration;
import hd3gtv.tools.Timecode;
import net.telestream.vantage.ws.ArrayOfstring;
import net.telestream.vantage.ws.Condition;
import net.telestream.vantage.ws.ConditionList;
import net.telestream.vantage.ws.ConditionValue;
import net.telestream.vantage.ws.Context;
import net.telestream.vantage.ws.IDomainGetWorkflowsUnlicensedSdkExceptionFaultFaultMessage;
import net.telestream.vantage.ws.IWorkflowSubmitSubmitFileUnlicensedSdkExceptionFaultFaultMessage;
import net.telestream.vantage.ws.IWorkflowSubmitSubmitFileWorkflowDoesNotExistExceptionFaultFaultMessage;
import net.telestream.vantage.ws.IWorkflowSubmitSubmitFileWorkflowInvalidStateExceptionFaultFaultMessage;
import net.telestream.vantage.ws.ObjectFactory;
import net.telestream.vantage.ws.Procedure;
import net.telestream.vantage.ws.SdkService;
import net.telestream.vantage.ws.TypeCode;

public class VantageAPI {
	
	private SdkService service;
	private List<Procedure> workflow_list;
	private ObjectFactory fact;
	
	public static VantageAPI createFromConfiguration() {
		try {
			return new VantageAPI(Configuration.global.getValue("vantage", "host", "127.0.0.1"));
		} catch (MalformedURLException e) {
			return null;
		}
	}
	
	public VantageAPI(String host) throws MalformedURLException {
		URL url = new URL("http://" + host + ":8676/");
		
		QName qname = new QName("http://tempuri.org/", "SdkService");
		service = new SdkService(url, qname);
		
		fact = new ObjectFactory();
	}
	
	private void setWorkflowList() throws IDomainGetWorkflowsUnlicensedSdkExceptionFaultFaultMessage {
		workflow_list = service.getDomain().getWorkflows().getProcedure();
	}
	
	private Procedure getWorkflowByName(String workflow_name) throws IDomainGetWorkflowsUnlicensedSdkExceptionFaultFaultMessage {
		if (workflow_list == null) {
			setWorkflowList();
		}
		return workflow_list.stream().filter(p -> {
			return p.getName().getValue().equalsIgnoreCase(workflow_name);
		}).findFirst().orElse(null);
	}
	
	private String getWorkflowIdentifier(Procedure workflow) {
		return workflow.getIdentifier();
	}
	
	/**
	 * @return Map<name, uuid>
	 */
	private Map<String, String> getVariablesIdForWorkflow(Procedure workflow) {
		if (workflow.getConditions().isNil()) {
			return Collections.emptyMap();
		}
		
		return workflow.getConditions().getValue().getCondition().stream().collect(Collectors.toMap(cdt -> {
			return ((Condition) cdt).getName().getValue();
		}, cdt -> {
			return ((Condition) cdt).getIdentifier(); // getInstance exists
		}));
	}
	
	public void printWorkflowList(PrintWriter pw) throws IDomainGetWorkflowsUnlicensedSdkExceptionFaultFaultMessage {
		if (workflow_list == null) {
			setWorkflowList();
		}
		workflow_list.forEach(p -> {
			pw.println("  - " + p.getName().getValue() + " [" + p.getIdentifier() + "]" + " " + p.getDescription().getValue()/* + " " + p.getLockState().value()*/);
			if (p.getConditions().isNil() == false) {
				p.getConditions().getValue().getCondition().forEach(cdt -> {
					pw.println("      Condition: " + cdt.getName().getValue() + ", [instance:" + cdt.getInstance() + "] [id:" + cdt.getIdentifier() + "]");
					if (cdt.getConditionValue().isNil() == false) {
						ConditionValue c_value = cdt.getConditionValue().getValue();
						if (c_value.getDefault().isNil() == false) {
							if (c_value.getDefault().getValue().getText().isNil() == false) {
								pw.println("         default " + c_value.getDefault().getValue().getText().getValue().getString());
							}
						}
					}
				});
			}
		});
	}
	
	/**
	 * @param source_file_unc like \\myserver\path\file.ext (JVM don't need to touch it)
	 * @param workflow_name the same in Workflow Designer
	 * @param job_name As you want
	 */
	public VantageJob createJob(String source_file_unc, String workflow_name, Collection<VariableDefinition> variables, String job_name) throws IWorkflowSubmitSubmitFileUnlicensedSdkExceptionFaultFaultMessage, IWorkflowSubmitSubmitFileWorkflowDoesNotExistExceptionFaultFaultMessage, IWorkflowSubmitSubmitFileWorkflowInvalidStateExceptionFaultFaultMessage, IDomainGetWorkflowsUnlicensedSdkExceptionFaultFaultMessage {
		Procedure workflow = getWorkflowByName(workflow_name);
		
		final ConditionList condition_list = new ConditionList();
		variables.forEach(variable -> {
			variable.toJobConditionList(workflow, condition_list);
		});
		
		Context myContext = new Context();
		myContext.setConditions(fact.createContextConditions(condition_list));
		
		String job_id = service.getSubmit().submitFile(getWorkflowIdentifier(workflow), source_file_unc, myContext, job_name);
		
		return new VantageJob(job_id, source_file_unc, workflow_name, job_name);
	}
	
	public class VantageJob {
		private String job_id;
		private String source_file_unc;
		private String workflow_name;
		private String job_name;
		
		private VantageJob(String job_id, String source_file_unc, String workflow_name, String job_name) {
			this.job_id = job_id;
			this.source_file_unc = source_file_unc;
			this.workflow_name = workflow_name;
			this.job_name = job_name;
		}
		// TODO (postponed) Watch Vantage job during restore
		
	}
	
	public VariableDefinition createVariableDef(String name, String value, TypeCode code) {
		VariableDefinition vd = new VariableDefinition(name);
		return vd.setValue(value, code);
	}
	
	public VariableDefinition createVariableDef(String name, String value) {
		VariableDefinition vd = new VariableDefinition(name);
		return vd.setValue(value, TypeCode.STRING);
	}
	
	public VariableDefinition createVariableDef(String name, int value) {
		VariableDefinition vd = new VariableDefinition(name);
		return vd.setValue(String.valueOf(value), TypeCode.INT_16);
	}
	
	public VariableDefinition createVariableDef(String name, Timecode value) {
		VariableDefinition vd = new VariableDefinition(name);
		return vd.setValue(value.toString(), TypeCode.TIME_CODE);
	}
	
	public VariableDefinition createVariableDef(String name, boolean value) {
		VariableDefinition vd = new VariableDefinition(name);
		return vd.setValue(String.valueOf(value), TypeCode.BOOLEAN);
	}
	
	public class VariableDefinition {
		private String name;
		private String value;
		private TypeCode type_code;
		
		private VariableDefinition(String name) {
			this.name = name;
			if (name == null) {
				throw new NullPointerException("\"name\" can't to be null");
			}
			if (name.isEmpty()) {
				throw new NullPointerException("\"name\" can't to be empty");
			}
		}
		
		private VariableDefinition setValue(String value, TypeCode type_code) {
			this.value = value;
			if (value == null) {
				throw new NullPointerException("\"value\" can't to be null");
			}
			if (value.isEmpty()) {
				throw new NullPointerException("\"value\" can't to be empty");
			}
			this.type_code = type_code;
			return this;
		}
		
		private void toJobConditionList(Procedure workflow, ConditionList conditionList) {
			Map<String, String> vars_id = getVariablesIdForWorkflow(workflow);
			
			Condition priorityCondition = new Condition();
			priorityCondition.setName(fact.createCollectionName(name));
			priorityCondition.setIdentifier(vars_id.get(name));
			priorityCondition.setTypeCode(type_code);
			
			ArrayOfstring myVal = new ArrayOfstring();
			myVal.getString().add(value);
			
			ConditionValue conditionValue = new ConditionValue();
			conditionValue.setText(fact.createValueText(myVal));
			
			/*Constraint myConstraint = new Constraint();
			myConstraint.setText(fact.createValueText(myVal));*/
			// conditionValue.setDefault(fact.createConditionValueDefault(myConstraint));
			
			priorityCondition.setConditionValue(fact.createConditionValue(conditionValue));
			conditionList.getCondition().add(priorityCondition);
		}
	}
	
}
