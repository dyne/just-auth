# Just Auth - A simple and generic two factor authentication lib

[![Build Status](https://travis-ci.org/Commonfare-net/just-auth.svg?branch=master)](https://travis-ci.org/Commonfare-net/just-auth)

## License


This Free and Open Source research and development activity is funded by the European Commission in the context of Collective Awareness Platforms for Sustainability and Social Innovation (CAPSSI).

## Running the tests

Freecoin comes complete with test units which are run by the CI but can also be run locally.

### Run all tests

For the purpose we use Clojure's `midje` package, to be run with:

```
lein midje
```

### Run only the fast tests

Some of the tests are marked as slow. If you want to avoid running them you cn either

`lein midje :filter -slow`

or use the alias

`lein test-basic`

The just auth lib is Copyright (C) 2017 by the Dyne.org Foundation, Amsterdam

The development is lead by Aspasia Beneti <aspra@dyne.org>

```
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```
