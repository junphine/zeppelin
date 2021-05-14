/*
 *  Copyright 2018 TWO SIGMA OPEN SOURCE, LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.twosigma.beakerx.widget;

import com.twosigma.beakerx.kernel.BeakerXJson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.twosigma.beakerx.widget.SparkUIApi.SPARK_ADVANCED_OPTIONS;
import static com.twosigma.beakerx.widget.SparkUIApi.SPARK_EXECUTOR_CORES;
import static com.twosigma.beakerx.widget.SparkUIApi.SPARK_EXECUTOR_MEMORY;
import static com.twosigma.beakerx.widget.SparkUIApi.SPARK_MASTER;

public class SparkUiDefaultsImpl implements SparkUiDefaults {

  public static final String SPARK_EXECUTOR_CORES_DEFAULT = "10";
  public static final String SPARK_EXECUTOR_MEMORY_DEFAULT = "8g";
  public static final String SPARK_MASTER_DEFAULT = "local[*]";

  public static final String NAME = "name";
  public static final String VALUE = "value";
  public static final String PROPERTIES = "properties";
  public static final String SPARK_OPTIONS = "spark_options";
  public static final String BEAKERX = "beakerx";
  private Map<String, Object> defaults = new HashMap<>();
  private List<Map<String, Object>> profiles = new ArrayList<>();

  private BeakerXJson beakerXJson;
  private String currentProfile = DEFAULT_PROFILE;

  public SparkUiDefaultsImpl(BeakerXJson beakerXJson) {
    this.beakerXJson = beakerXJson;
  }

  public void saveSparkConf(List<Map<String, Object>> profiles) {
    Map<String, Map> map = beakerXJson.beakerxJsonAsMap();
    Map<String, Object> sparkOptions = (Map<String, Object>) map.get(BEAKERX).getOrDefault(SPARK_OPTIONS, new HashMap<>());
    sparkOptions.put(SPARK_PROFILES, profiles == null ? new ArrayList<>() : profiles);
    map.get(BEAKERX).put(SPARK_OPTIONS, sparkOptions);
    beakerXJson.save(map);
    this.profiles = profiles;
  }

  @Override
  public void loadDefaults() {
    loadProfiles();
    this.defaults = getProfileByName(currentProfile);
  }

  @Override
  public List<Map<String, Object>> getProfiles() {
    return profiles;
  }

  public Map<String, Object> getProfileByName(String name) {
    return new HashMap<>(profiles.stream().filter(x -> x.get("name").equals(name)).findFirst().orElse(new HashMap<>()));
  }

  @Override
  public void loadProfiles() {
    Map<String, Map> beakerxJsonAsMap = beakerXJson.beakerxJsonAsMap();
    Map sparkOptions = (Map) beakerxJsonAsMap.get(BEAKERX).getOrDefault(SPARK_OPTIONS, new HashMap<>());
    List<Map<String, Object>> profiles = (List<Map<String, Object>>) sparkOptions.get(SPARK_PROFILES);
    this.currentProfile = (String) sparkOptions.getOrDefault(CURRENT_PROFILE, DEFAULT_PROFILE);
    if (profiles == null) {
      //save default config if doesn't exist
      Map<String, Object> defaultProfile = new HashMap<>();
      defaultProfile.put("name", DEFAULT_PROFILE);
      defaultProfile.put(SPARK_MASTER, SPARK_MASTER_DEFAULT);
      defaultProfile.put(SPARK_EXECUTOR_CORES, SPARK_EXECUTOR_CORES_DEFAULT);
      defaultProfile.put(SPARK_EXECUTOR_MEMORY, SPARK_EXECUTOR_MEMORY_DEFAULT);
      defaultProfile.put(SPARK_ADVANCED_OPTIONS, new ArrayList<>());
      saveProfiles(Arrays.asList(defaultProfile));
    } else {
      this.profiles = profiles;
    }
  }

  @Override
  public void saveProfiles(List<Map<String, Object>> profiles) {
    this.profiles = profiles;
    saveSparkConf(profiles);

  }

  @Override
  public void saveCurrentProfileName(String profileName) {
    Map<String, Map> map = beakerXJson.beakerxJsonAsMap();
    Map<String, Object> sparkOptions = (Map<String, Object>) map.get(BEAKERX).getOrDefault(SPARK_OPTIONS, new HashMap<>());
    sparkOptions.put(CURRENT_PROFILE, profileName);
    map.get(BEAKERX).put(SPARK_OPTIONS, sparkOptions);
    beakerXJson.save(map);
    currentProfile = profileName;
  }

  @Override
  public String getCurrentProfileName() {
    return currentProfile;
  }

  @Override
  public Object get(String key) {
    return this.defaults.get(key);
  }

  @Override
  public Map<String, String> getProperties() {
    Map<String, String> result = new HashMap<>();
    if (defaults.containsKey(PROPERTIES)) {
      List<Map<String, String>> props = (List<Map<String, String>>) defaults.get(PROPERTIES);
      props.forEach(x -> {
        String pname = x.get(SparkUiDefaultsImpl.NAME);
        String pvalue = x.get(SparkUiDefaultsImpl.VALUE);
        if ((pname != null && !pname.isEmpty()) && (pvalue != null && !pvalue.isEmpty())) {
          result.put(pname, pvalue);
        }
      });
    }
    return result;
  }

  @Override
  public void removeSparkConf(String profileName) {
    profiles.removeIf(x -> x.get("name").equals(profileName));
    saveSparkConf(profiles);
  }

}
