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

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.Module;

import br.com.anteros.nosql.persistence.session.NoSQLSessionFactory;



public class AnterosPersistenceJacksonModule extends Module {

	public enum Feature {
		/**
		 * Indica se o objeto com lazy load deve ser carregado para e depois
		 * serializado ou serializado como nulo.
		 * <p>
		 * Valor padrão é falso.
		 */
		FORCE_LAZY_LOADING(false),

		/**
		 * Indica se a anotação
		 * {@link br.com.anteros.persistence.metadata.annotation.Transient} deve
		 * ser verificada ou não; se verdadeiro, irá considerar
		 * 
		 * @Transient para dizer se a propriedade deve ser ignorada; se falso a
		 *            anotação não terá nenhum efeito.
		 *            <p>
		 *            Valor padrão é verdadeiro.
		 */
		USE_TRANSIENT_ANNOTATION(true),

		/**
		 * This feature determines how
		 * {@link org.hibernate.collection.PersistentCollection}s properties for
		 * which no annotation is found are handled with respect to
		 * lazy-loading: if true, lazy-loading is only assumed if annotation is
		 * used to indicate that; if false, lazy-loading is assumed to be the
		 * default. Note that {@link #FORCE_LAZY_LOADING} has priority over this
		 * Feature; meaning that if it is defined as true, setting of this
		 * Feature has no effect.
		 * <p>
		 * Default value is false, meaning that laziness is considered to be the
		 * default value.
		 * 
		 * @since 2.4
		 */
		REQUIRE_EXPLICIT_LAZY_LOADING_MARKER(false), ;

		final boolean _defaultState;
		final int _mask;

		/**
		 * Method that calculates bit set (flags) of all features that are
		 * enabled by default.
		 */
		public static int collectDefaults() {
			int flags = 0;
			for (Feature f : values()) {
				if (f.enabledByDefault()) {
					flags |= f.getMask();
				}
			}
			return flags;
		}

		private Feature(boolean defaultState) {
			_defaultState = defaultState;
			_mask = (1 << ordinal());
		}

		public boolean enabledIn(int flags) {
			return (flags & _mask) != 0;
		}

		public boolean enabledByDefault() {
			return _defaultState;
		}

		public int getMask() {
			return _mask;
		}
	}

	protected final static int DEFAULT_FEATURES = Feature.collectDefaults();

	protected int _moduleFeatures = DEFAULT_FEATURES;

	private DeserializationContext deserializationContext;

	private NoSQLSessionFactory sessionFactory;

	private AnterosSerializerModifier anterosSerializerModifier;

	private AnterosSerializers anterosSerializers;

	public AnterosPersistenceJacksonModule(NoSQLSessionFactory sessionFactory,
			DeserializationContext deserializationContext) {
		this.deserializationContext = deserializationContext;
		this.sessionFactory = sessionFactory;
	}

	@Override
	public String getModuleName() {
		{
			return "jackson-datatype-anteros";
		}
	}

	@Override
	public Version version() {
		{
			return ModuleVersion.instance.version();
		}
	}

	@Override
	public void setupModule(SetupContext context) {
		context.appendAnnotationIntrospector(annotationIntrospector());
		anterosSerializerModifier = new AnterosSerializerModifier(_moduleFeatures, sessionFactory);
		anterosSerializers = new AnterosSerializers(_moduleFeatures);
		context.addBeanSerializerModifier(anterosSerializerModifier);
		context.addSerializers(anterosSerializers);
	}

	protected AnnotationIntrospector annotationIntrospector() {
		AnterosAnnotationIntrospector ai = new AnterosAnnotationIntrospector(sessionFactory);
		ai.setUseTransient(isEnabled(Feature.USE_TRANSIENT_ANNOTATION));
		return ai;
	}

	public AnterosPersistenceJacksonModule enable(Feature f) {
		_moduleFeatures |= f.getMask();
		if (anterosSerializerModifier != null)
			anterosSerializerModifier.setFeatures(_moduleFeatures);
		if (anterosSerializers!=null)
			anterosSerializers.setForceLoading(Feature.FORCE_LAZY_LOADING.enabledIn(_moduleFeatures));
		return this;
	}

	public AnterosPersistenceJacksonModule disable(Feature f) {
		_moduleFeatures &= ~f.getMask();
		if (anterosSerializerModifier != null)
			anterosSerializerModifier.setFeatures(_moduleFeatures);
		return this;
	}

	public final boolean isEnabled(Feature f) {
		return (_moduleFeatures & f.getMask()) != 0;
	}

	public AnterosPersistenceJacksonModule configure(Feature f, boolean state) {
		if (state) {
			enable(f);
		} else {
			disable(f);
		}
		return this;
	}
}
