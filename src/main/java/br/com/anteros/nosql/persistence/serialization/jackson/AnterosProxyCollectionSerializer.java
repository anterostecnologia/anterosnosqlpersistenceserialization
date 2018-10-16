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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import br.com.anteros.nosql.persistence.metadata.annotations.Reference;
import br.com.anteros.nosql.persistence.proxy.AnterosPersistentCollection;
import br.com.anteros.nosql.persistence.proxy.AnterosProxyObject;
import br.com.anteros.nosql.persistence.serialization.jackson.AnterosPersistenceJacksonModule.Feature;
import br.com.anteros.nosql.persistence.session.NoSQLSessionFactory;


public class AnterosProxyCollectionSerializer extends JsonSerializer<Object> implements ContextualSerializer {
	protected final int _features;

	protected final JsonSerializer<Object> _serializer;
	protected final NoSQLSessionFactory _sessionFactory;

	@SuppressWarnings("unchecked")
	public AnterosProxyCollectionSerializer(JsonSerializer<?> serializer, int features, NoSQLSessionFactory sessionFactory) {
		_serializer = (JsonSerializer<Object>) serializer;
		_features = features;
		_sessionFactory = sessionFactory;
	}

	@Override
	public boolean isEmpty(Object value) {
		if (value == null) {
			return true;
		}
		if (AnterosProxyObject.class.isAssignableFrom(value.getClass())) {

		} else if (value instanceof AnterosPersistentCollection) {
			return findLazyValue((AnterosPersistentCollection) value) == null;
		}
		return false;
	}

	@Override
	public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
			JsonProcessingException {
		if (AnterosProxyObject.class.isAssignableFrom(value.getClass())) {
			Object proxiedValue;
			try {
				proxiedValue = findProxied(value);
			} catch (Exception e) {
				throw new JacksonSerializationException(e);
			}
			value = proxiedValue;
			if (value == null) {
				provider.defaultSerializeNull(jgen);
				return;
			}
		} else if (value instanceof AnterosPersistentCollection) {
			AnterosPersistentCollection coll = (AnterosPersistentCollection) value;
			if (!Feature.FORCE_LAZY_LOADING.enabledIn(_features) && !coll.isInitialized()) {
				provider.defaultSerializeNull(jgen);
				return;
			}
			value = coll;
			if (value == null) {
				provider.defaultSerializeNull(jgen);
				return;
			}
		}
		if (_serializer == null) {
			throw new JsonMappingException("PersistentCollection does not have serializer set");
		}
		_serializer.serialize(value, jgen, provider);
	}

	@Override
	public void serializeWithType(Object value, JsonGenerator jgen, SerializerProvider provider, TypeSerializer typeSer)
			throws IOException, JsonProcessingException {
		if (AnterosProxyObject.class.isAssignableFrom(value.getClass())) {
			Object proxiedValue;
			try {
				proxiedValue = findProxied(value);
			} catch (Exception e) {
				throw new JacksonSerializationException(e);
			}
			value = proxiedValue;
			if (value == null) {
				provider.defaultSerializeNull(jgen);
				return;
			}
		} else if (value instanceof AnterosPersistentCollection) {
			AnterosPersistentCollection coll = (AnterosPersistentCollection) value;
			if (!Feature.FORCE_LAZY_LOADING.enabledIn(_features) && !coll.isInitialized()) {
				provider.defaultSerializeNull(jgen);
				return;
			}
			value = coll;
			if (value == null) {
				provider.defaultSerializeNull(jgen);
				return;
			}
		}
		if (_serializer == null) {
			throw new JsonMappingException("PersistentCollection does not have serializer set");
		}
		_serializer.serializeWithType(value, jgen, provider, typeSer);
	}

	protected Object findLazyValue(AnterosPersistentCollection coll) {
		if (!Feature.FORCE_LAZY_LOADING.enabledIn(_features) && !coll.isInitialized()) {
			return null;
		}

		if (_sessionFactory != null) {
			coll.initialize();
		}

		return coll;
	}

	protected boolean usesLazyLoading(BeanProperty property) {
		if (property != null) {
			Reference reference = property.getAnnotation(Reference.class);
			if (reference != null) {
				return reference.lazy();
			}
			return !Feature.REQUIRE_EXPLICIT_LAZY_LOADING_MARKER.enabledIn(_features);
		}
		return false;
	}

	public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property)
			throws JsonMappingException {
		JsonSerializer<?> ser = provider.handlePrimaryContextualization(_serializer, property);
		if (!usesLazyLoading(property)) {
			return ser;
		}
		if (ser != _serializer) {
			return new AnterosProxyCollectionSerializer(ser, _features, _sessionFactory);
		}
		return this;
	}

	protected Object findProxied(Object value) throws Exception {
		if (value instanceof AnterosProxyObject) {
			if (!Feature.FORCE_LAZY_LOADING.enabledIn(_features)
					&& !((AnterosProxyObject) (value)).isInitialized()) {
				return null;
			}
			return ((AnterosProxyObject) (value)).initializeAndReturnObject();
		}
		
		return null;
	}
}