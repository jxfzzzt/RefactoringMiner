/**
 *  Copyright 2012-2014 Gunnar Morling (http://www.gunnarmorling.de/)
 *  and/or other contributors as indicated by the @authors tag. See the
 *  copyright.txt file in the distribution for a full listing of all
 *  contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.mapstruct.ap.test.nestedmethodcall;

import java.util.List;
import javax.xml.bind.JAXBElement;

/**
 * @author Sjaak Derksen
 */
public class OrderDetailsType {

    private JAXBElement<String> name;
    private List<JAXBElement<String>> description;

    public JAXBElement<String> getName() {
        return name;
    }

    public void setName(JAXBElement<String> value) {
        this.name = value;
    }

    public void setDescription(List<JAXBElement<String>> description) {
        this.description = description;
    }

    public List<JAXBElement<String>> getDescription() {
        return description;
    }

}
