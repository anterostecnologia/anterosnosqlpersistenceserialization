/*******************************************************************************
 * Copyright 2012 Anteros Tecnologia
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package br.com.anteros.nosql.persistence.serialization.jackson;

import java.text.SimpleDateFormat;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import br.com.anteros.nosql.persistence.serialization.jackson.AnterosPersistenceJacksonModule.Feature;
import br.com.anteros.nosql.persistence.session.NoSQLSessionFactory;

public class AnterosNoSQLObjectMapper extends ObjectMapper {

	private static final long serialVersionUID = 1L;

	private AnterosPersistenceJacksonModule module;
	private NoSQLSessionFactory sessionFactory;

	public AnterosNoSQLObjectMapper(NoSQLSessionFactory sessionFactory) {
		super();
		this.sessionFactory = sessionFactory;
		this.createModule();
		this.registerModule(module);
		this.enable(SerializationFeature.INDENT_OUTPUT);
		this.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		this.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"));
		this.setSerializationInclusion(Include.NON_NULL);
		this.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
	}

	public AnterosPersistenceJacksonModule getModule() {
		return module;
	}
	
	public AnterosNoSQLObjectMapper enable(Feature feature){
		module.enable(feature);
		this.registerModule(module);
		return this;
	}
	
	public AnterosNoSQLObjectMapper disable(Feature feature){
		module.disable(feature);
		this.registerModule(module);
		return this;
	}
	
	protected void createModule(){
		this.module = new AnterosPersistenceJacksonModule(sessionFactory, this.getDeserializationContext());
	}
	

}
