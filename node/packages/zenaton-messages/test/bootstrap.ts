import 'jest-extended';

expect.extend({
  toBeOfType(received: any, type: string) {
    const pass = typeof received === type;
    if (pass) {
      return {
        message: () => `expected ${received} not to be of type ${type}`,
        pass: true,
      };
    } else {
      return {
        message: () => `expected ${received} to be of type ${type}`,
        pass: false,
      };
    }
  },

  toBeOfTypeOrNull(received: any, type: string) {
    const pass = typeof received === type || received === null;
    if (pass) {
      return {
        message: () => `expected ${received} not to be of type ${type} or null`,
        pass: true,
      };
    } else {
      return {
        message: () => `expected ${received} to be of type ${type} or null`,
        pass: false,
      };
    }
  },
});

declare global {
  namespace jest {
    interface Matchers<R> {
      toBeOfType(type: string): CustomMatcherResult;
      toBeOfTypeOrNull(type: string): CustomMatcherResult;
    }
  }
}

export {};
