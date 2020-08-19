declare global {
  namespace jest {
    interface Matchers<R> {
      toBeOfType(type: string): CustomMatcherResult;
      toBeOfTypeOrNull(type: string): CustomMatcherResult;
    }
  }
}

export {};
