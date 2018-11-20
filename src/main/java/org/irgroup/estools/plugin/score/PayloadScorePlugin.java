/*
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
 */

package org.irgroup.estools.plugin.score;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.script.ScoreScript;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.ScriptEngine;

import java.util.Collection;
import java.util.Map;

/**
 * payload score ranking plugin. only Elasticsearch 6.5 or higher
 */
public class PayloadScorePlugin extends Plugin implements ScriptPlugin {

	@Override
	public ScriptEngine getScriptEngine(Settings settings, Collection<ScriptContext<?>> contexts) {
		return new WmpScriptEngine();
	}

	private static class WmpScriptEngine implements ScriptEngine {
		private final String _SOURCE_VALUE = "payload_score";
		private final String _LANG_VALUE = "irgroup";

		@Override
		public String getType() {
			return _LANG_VALUE;
		}

		@Override
		public <T> T compile(String scriptName, String scriptSource, ScriptContext<T> context, Map<String, String> params) {

			if (!context.equals(ScoreScript.CONTEXT)) {
				throw new IllegalArgumentException(getType()
						+ " scripts cannot be used for context ["
						+ context.name + "]");
			}

			// we use the script "source" as the script identifier
			if (_SOURCE_VALUE.equals(scriptSource)) {
				ScoreScript.Factory factory = PayloadScoreFactory::new;
				return context.factoryClazz.cast(factory);
			}
			throw new IllegalArgumentException("Unknown script name " + scriptSource);
		}

		@Override
		public void close() {
			// optionally close resources
		}


	}
}
