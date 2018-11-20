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

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.script.ScoreScript;
import org.elasticsearch.search.lookup.SearchLookup;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;


public final class PayloadScoreFactory implements ScoreScript.LeafFactory {
	private final Map<String, Object> params;
	private final SearchLookup lookup;
	private final String field;
	private final String term;

	public PayloadScoreFactory(Map<String, Object> params, SearchLookup lookup) {

		if (params.containsKey("field") == false) {
			throw new IllegalArgumentException("Missing parameter [field]");
		}
		if (params.containsKey("term") == false) {
			throw new IllegalArgumentException("Missing parameter [term]");
		}
		this.params = params;
		this.lookup = lookup;
		field = params.get("field").toString();
		term = params.get("term").toString();
	}

	@Override
	public boolean needs_score() {
		return false;
	}

	@Override
	public ScoreScript newInstance(LeafReaderContext context) throws IOException {

		PostingsEnum postings = context.reader().postings(new Term(field, term), PostingsEnum.PAYLOADS);

		if (postings == null) {
			return new ScoreScript(params, lookup, context) {
				@Override
				public double execute() {
					return 0.0d;
				}
			};
		}

		return new ScoreScript(params, lookup, context) {
			int currentDocid = -1;

			@Override
			public void setDocument(int docid) {
				/*
				 * advance has undefined behavior calling with
				 * a docid <= its current docid
				 */
				if (postings.docID() < docid) {
					try {
						postings.advance(docid);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}
				currentDocid = docid;
			}

			@Override
			public double execute() {

				if (postings.docID() != currentDocid) {
					/*
					 * advance moved past the current doc, so this doc
					 * has no occurrences of the term
					 */
					return 0.0d;
				}
				try {
					int freq = postings.freq();
					float sum_payload = 0.0f;
					for(int i = 0; i < freq; i ++)
					{
						postings.nextPosition();
						BytesRef payload = postings.getPayload();
						if(payload != null) {
							sum_payload += ByteBuffer.wrap(payload.bytes, payload.offset, payload.length)
									.order(ByteOrder.BIG_ENDIAN).getFloat();
						}
					}

					return sum_payload;

				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}

		};
	}
}