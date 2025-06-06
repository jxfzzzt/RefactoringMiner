package org.rapidoid.widget;

/*
 * #%L
 * rapidoid-widget
 * %%
 * Copyright (C) 2014 - 2015 Nikolche Mihajlovski and contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.html.Tag;
import org.junit.Test;

@Authors("Nikolche Mihajlovski")
@Since("2.0.0")
public class PlaygroundWidgetTest extends WidgetTestCommons {

	private static final String ATTRS = "[^>]*?";

	@Test
	public void testPlaygroundWidget() {
		setupMockExchange();

		Tag play = PlaygroundWidget.pageContent(null);
		print(play);

		hasRegex(play, "<table class=\"table" + ATTRS + ">");

		hasRegex(play, "<button[^>]*?>\\-</button>");
		hasRegex(play, "<span[^>]*?>10</span>");
		hasRegex(play, "<button[^>]*?>\\+</button>");

		hasRegex(play, "<input [^>]*?style=\"border: 1px;\">");
	}

}
