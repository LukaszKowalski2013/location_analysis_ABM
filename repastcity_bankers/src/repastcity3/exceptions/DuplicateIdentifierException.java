/*ęCopyright 2019 Lukasz Kowalski
This file is part of Location Analysis ABM, which was built based on RepastCity software.
More information about the model can be found here:
https://github.com/LukaszKowalski2013/location_analysis_ABM
Polish readers can read my PhD thesis about it, that is available on my Research Gate profile: https://goo.gl/TViW89

ęCopyright 2012 Nick Malleson
RepastCity is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

RepastCity is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with RepastCity.  If not, see <http://www.gnu.org/licenses/>.*/
package repastcity3.exceptions;

/**
 * Exception is thrown when an object tries to set a unique identifier that has already
 * been used.
 * @author Nick Malleson
 */
public class DuplicateIdentifierException extends Exception {

	private static final long serialVersionUID = 1L;

	public DuplicateIdentifierException(String message) {
		super(message);
	}

}
