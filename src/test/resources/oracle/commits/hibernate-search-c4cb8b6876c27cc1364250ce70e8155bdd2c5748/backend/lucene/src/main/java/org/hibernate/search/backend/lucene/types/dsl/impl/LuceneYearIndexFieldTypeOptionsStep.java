/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.Year;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneYearFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultParseConverters;

class LuceneYearIndexFieldTypeOptionsStep
		extends AbstractLuceneTemporalIndexFieldTypeOptionsStep<LuceneYearIndexFieldTypeOptionsStep, Year> {

	LuceneYearIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Year.class, DefaultParseConverters.YEAR );
	}

	@Override
	protected LuceneYearIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<Year, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, Year indexNullAsValue) {
		return new LuceneYearFieldCodec( indexing, docValues, storage, indexNullAsValue );
	}
}
