export class GenericError extends Error {
  constructor(message?: string) {
    if (message === undefined) {
      message = "There was an error while loading data from the API.";
    }

    super(message);
  }

  static withMessage(message: string): GenericError {
    return new GenericError(message);
  }
}
