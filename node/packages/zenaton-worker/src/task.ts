export interface Task {
  name: string;
  handle(...inputs: any[]): any;
}
