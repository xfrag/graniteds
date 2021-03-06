/*
  GRANITE DATA SERVICES
  Copyright (C) 2013 GRANITE DATA SERVICES S.A.S.

  This file is part of Granite Data Services.

  Granite Data Services is free software; you can redistribute it and/or modify
  it under the terms of the GNU Library General Public License as published by
  the Free Software Foundation; either version 2 of the License, or (at your
  option) any later version.

  Granite Data Services is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE. See the GNU Library General Public License
  for more details.

  You should have received a copy of the GNU Library General Public License
  along with this library; if not, see <http://www.gnu.org/licenses/>.
*/

package org.granite.messaging.amf.types;

import java.util.Calendar;
import java.util.Date;

import org.w3c.dom.Document;

/**
 * @author Franck WOLFF
 */
public class AMFBasicVectorObjectAliaser implements AMFVectorObjectAliaser {

	public String aliasFor(Class<?> cls) {
		if (cls == String.class || cls == Character.class)
			return "String";
		if (Date.class.isAssignableFrom(cls) || Calendar.class.isAssignableFrom(cls))
			return "Date";
		if (Document.class.isAssignableFrom(cls))
			return "XML";
		if (cls == Object.class)
			return "Object";
		return cls.getName();
	}
}
