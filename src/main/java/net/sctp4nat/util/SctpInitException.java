/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sctp4nat.util;

/**
 * @author jonaswagner
 *
 *         This Exception signals that the user tried to call Sctp.init() twice
 *         or more.
 *
 */
public class SctpInitException extends Exception {

	private static final long serialVersionUID = 1L;

	public SctpInitException(String string) {
		super(string);
	}

}
