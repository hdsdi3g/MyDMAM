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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.db.orm;

import java.nio.ByteBuffer;
import java.util.Date;

import com.netflix.astyanax.query.IndexQuery;
import com.netflix.astyanax.serializers.BooleanSerializer;
import com.netflix.astyanax.serializers.ByteBufferSerializer;
import com.netflix.astyanax.serializers.BytesArraySerializer;
import com.netflix.astyanax.serializers.DateSerializer;
import com.netflix.astyanax.serializers.DoubleSerializer;
import com.netflix.astyanax.serializers.IntegerSerializer;
import com.netflix.astyanax.serializers.LongSerializer;
import com.netflix.astyanax.serializers.StringSerializer;

public class Expression {
	
	private ExpressionOperator operator;
	
	private String fieldname;
	
	private ByteBuffer value;
	
	private void init(String fieldname, ExpressionOperator operator) {
		if (fieldname == null) {
			throw new NullPointerException("\"fieldname\" can't to be null");
		}
		if (operator == null) {
			throw new NullPointerException("\"operator\" can't to be null");
		}
		this.fieldname = fieldname;
		this.operator = operator;
	}
	
	public static Expression create(String fieldname, ExpressionOperator operator, boolean value) {
		Expression ciqe = new Expression();
		ciqe.init(fieldname, operator);
		ciqe.value = BooleanSerializer.get().toByteBuffer(value);
		return ciqe;
	}
	
	public static Expression create(String fieldname, ExpressionOperator operator, byte[] value) {
		Expression ciqe = new Expression();
		ciqe.init(fieldname, operator);
		if (value == null) {
			throw new NullPointerException("\"value\" can't to be null");
		}
		ciqe.value = BytesArraySerializer.get().toByteBuffer(value);
		return ciqe;
	}
	
	public static Expression create(String fieldname, ExpressionOperator operator, double value) {
		Expression ciqe = new Expression();
		ciqe.init(fieldname, operator);
		ciqe.value = DoubleSerializer.get().toByteBuffer(value);
		return ciqe;
	}
	
	public static Expression create(String fieldname, ExpressionOperator operator, int value) {
		Expression ciqe = new Expression();
		ciqe.init(fieldname, operator);
		ciqe.value = IntegerSerializer.get().toByteBuffer(value);
		return ciqe;
	}
	
	public static Expression create(String fieldname, ExpressionOperator operator, String value) {
		Expression ciqe = new Expression();
		ciqe.init(fieldname, operator);
		if (value == null) {
			throw new NullPointerException("\"value\" can't to be null");
		}
		ciqe.value = StringSerializer.get().toByteBuffer(value);
		return ciqe;
	}
	
	public static Expression create(String fieldname, ExpressionOperator operator, ByteBuffer value) {
		Expression ciqe = new Expression();
		ciqe.init(fieldname, operator);
		if (value == null) {
			throw new NullPointerException("\"value\" can't to be null");
		}
		ciqe.value = ByteBufferSerializer.get().toByteBuffer(value);
		return ciqe;
	}
	
	public static Expression create(String fieldname, ExpressionOperator operator, long value) {
		Expression ciqe = new Expression();
		ciqe.init(fieldname, operator);
		ciqe.value = LongSerializer.get().toByteBuffer(value);
		return ciqe;
	}
	
	public static Expression create(String fieldname, ExpressionOperator operator, Date value) {
		Expression ciqe = new Expression();
		ciqe.init(fieldname, operator);
		if (value == null) {
			throw new NullPointerException("\"value\" can't to be null");
		}
		ciqe.value = DateSerializer.get().toByteBuffer(value);
		return ciqe;
	}
	
	public static Expression create(String fieldname, ExpressionOperator operator, Object value) {
		Expression ciqe = new Expression();
		ciqe.init(fieldname, operator);
		if (value == null) {
			throw new NullPointerException("\"value\" can't to be null");
		}
		ciqe.value = StringSerializer.get().toByteBuffer(CassandraOrm.serialize(value));
		return ciqe;
	}
	
	/**
	 * ALWAYS ADD An equals WITH greater/less
	 * @param idx_rows
	 * @param orm
	 */
	void applyExpression(IndexQuery<String, String> idx_rows, CassandraOrm<?> orm) {
		switch (operator) {
		case equals:
			idx_rows.addExpression().whereColumn(fieldname).equals().value(value);
			break;
		case greaterthan:
			idx_rows.addExpression().whereColumn(fieldname).greaterThan().value(value);
			break;
		case greaterthanequals:
			idx_rows.addExpression().whereColumn(fieldname).greaterThanEquals().value(value);
			break;
		case lessthan:
			idx_rows.addExpression().whereColumn(fieldname).lessThan().value(value);
			break;
		case lessthanequals:
			idx_rows.addExpression().whereColumn(fieldname).lessThanEquals().value(value);
			break;
		default:
			return;
		}
	}
}
