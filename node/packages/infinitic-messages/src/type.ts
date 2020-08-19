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
