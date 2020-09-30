/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

import { Type as AvscType, Resolver } from 'avsc';
import { readFileSync } from 'fs';

/**
 * A generic Type class to provide proper typing when using avsc.
 */
export class Type<T> extends AvscType {
  fromBuffer(buffer: Buffer, resolver?: Resolver, noCheck?: boolean): T {
    return super.fromBuffer(buffer, resolver, noCheck);
  }

  fromString(str: string): T {
    return super.fromString(str);
  }

  toBuffer(value: T): Buffer {
    return super.toBuffer(value);
  }

  toString(val?: T): string {
    return super.toString(val);
  }
}

export type AvroRegistry = { [name: string]: AvscType };

export function typeForSchema<T>(file: string, registry: AvroRegistry) {
  const fileContent = readFileSync(file);
  const parsedContent = JSON.parse(fileContent.toString());

  return AvscType.forSchema(parsedContent, {
    registry: registry,
  }) as Type<T>;
}
