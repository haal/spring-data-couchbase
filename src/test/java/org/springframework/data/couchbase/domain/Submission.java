/*
 * Copyright 2020 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.couchbase.domain;

import java.lang.reflect.Field;

/**
 * Submission entity for tests
 *
 * @author Michael Reiche
 */
public class Submission {
	private final String id;
	private final String userId;
	private final String talkId;
	private final String status;
	private final long number;

	public Submission(String id, String userId, String talkId, String status, long number) {
		this.id = id;
		this.userId = userId;
		this.talkId = talkId;
		this.status = status;
		this.number = number;
	}

	public String getId() {
		return id;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("Submission(");
		sb.append("id=");
		sb.append(id);
		sb.append(", userId=");
		sb.append(userId);
		sb.append(", talkId=");
		sb.append(talkId);
		sb.append(", status=");
		sb.append(status);
		sb.append(", number=");
		sb.append(number);
		sb.append(")");
		return sb.toString();
	}

	@Override
	public boolean equals(Object that) throws RuntimeException {
		if (this == that)
			return true;
		if (that == null || getClass() != that.getClass())
			return false;
		for (Field f : getClass().getFields()) {
			if (!same(f, this, that))
				return false;
		}
		for (Field f : getClass().getDeclaredFields()) {
			if (!same(f, this, that))
				return false;
		}
		return true;
	}

	private static boolean same(Field f, Object a, Object b) {
		Object thisField = null;
		Object thatField = null;

		try {
			f.get(a);
			f.get(b);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		if (thisField == null && thatField == null) {
			return true;
		}
		if (thisField == null && thatField != null) {
			return false;
		}
		if (!thisField.equals(thatField)) {
			return false;
		}
		return true;
	}
}
