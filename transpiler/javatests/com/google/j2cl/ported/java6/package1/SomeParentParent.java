/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.ported.java6.package1;

import com.google.j2cl.ported.java6.package3.SomePackageConfusedParent;

/** Class with just a package private method that returns its name. */
public class SomeParentParent extends SomePackageConfusedParent {

  // A package private method that is not live.
  String f() {
    return "Method not live";
  }

  @Override
  String m() {
    return "SomeParentParent";
  }
}
