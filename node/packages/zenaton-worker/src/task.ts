export interface Task<I = any, O = any> {
  name: string;
  handle(input: I): O;
}
