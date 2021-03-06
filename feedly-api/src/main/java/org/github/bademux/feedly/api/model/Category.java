/*
 * Copyright 2013 Bademus
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
 *
 *    Contributors:
 *                 Bademus
 */

package org.github.bademux.feedly.api.model;

import com.google.api.client.util.Key;

import java.util.ArrayList;


public class Category extends IdGenericJson implements Markable, Stream {

  public static final String PREFIX = "category";

  /** Users can promote sources they really love to read to must have. */
  public static final String MUST = "global.must";
  /** All the articles from all the sources the user subscribes to. */
  public static final String ALL = "global.all";
  /** All the articles from all the sources the user subscribes to and are not in a category. */
  public static final String UNCATEGORIZED = "global.uncategorized";

  @Key
  private String label;

  /**
   * @hide
   * Use org.github.bademux.feedly.api.service.Feedly#newCategory(java.lang.String)
   */
  public Category(String name, String userId) {
    super(PREFIX, name, userId);
    this.label = name;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(final String label) {
    this.label = label;
  }

  public Category() {super(PREFIX);}

  @Override
  public Category set(String fieldName, Object value) {
    return (Category) super.set(fieldName, value);
  }

  @Override
  public Category clone() { return (Category) super.clone(); }

  @SuppressWarnings("serial")
  public static class Categories extends ArrayList<Category> {}
}


