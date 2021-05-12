/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {
    SpellBase,
    SpellResult,
    DefaultDisplayType,
} from 'zeppelin-spell';

import 'whatwg-fetch';
import md5 from 'js-md5';

export default class TranslatorSpell extends SpellBase {
    constructor() {
        super("%translator");
    }

    /**
     * Consumes text and return `SpellResult`.
     *
     * @param paragraphText {string} which doesn't include magic
     * @param config {Object}
     * @return {SpellResult}
     */
    interpret(paragraphText, config) {
        const parsed = this.parseConfig(paragraphText);
        const auth = config['access-token'];
        const key = config['access-key'];
        const source = parsed.source;
        const target = parsed.target;
        const text = parsed.text;

        /**
         * SpellResult.add()
         * - accepts not only `string` but also `promise` as a parameter
         * - allows to add multiple output using the `add()` function
         */
        const result = new SpellResult()
            .add('<h4>Translation Result</h4>', DefaultDisplayType.HTML)
            // or use display system implicitly like
            // .add('%html <h4>Translation From English To Korean</h4>')
            .add(this.translate(source, target, key, auth, text));
        return result;
    }

    parseConfig(text) {
        const pattern = /^\s*(\S+)\s+(\S+)\s+([\S\s]*)/g;
        const match = pattern.exec(text);

        if (!match) {
            throw new Error(`Failed to parse configuration. See README`);
        }

        return {
            source: match[1],
            target: match[2],
            text: match[3],
        }
    }

    translate(source, target, key, pid, text) {
    	const salt = 'zeppelin';    	
    	const sign = md5(pid + text + salt + key);
        return fetch('http://fanyi.sogou.com/reventondc/api/sogouTranslate', {
            method: 'POST',
            headers: {
            	'Accept': 'application/json',
                'Content-Type': 'application/x-www-form-urlencoded'                
            },
            body: this.queryStringify({
                'q': text,
                'from': source,
                'to': target,
                'pid': pid,
                'salt': salt,
                'sign': sign
            })
        }).then(response => {
            if (response.status === 200) {
                return response.json()
            }
            throw new Error(`http://fanyi.sogou.com/reventondc/api/sogouTranslate ${response.status} (${response.statusText})`);
        }).then((json) => {
        	if(json.message){
        		return json.message;
        	}
            const extracted = json.translation
            return extracted;
        });
    }
    
    queryStringify(data){
    	var query = '';
    	for(var n in data){
    		if(query){
    			query+='&';
    		}
    		query+=n+'='+data[n];
    	}
    	return query;
    }
}

