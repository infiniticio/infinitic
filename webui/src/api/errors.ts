export class GenericError extends Error {
  constructor(message?: string) {
    if (message === undefined) {
      message = "There was an error while loading data from the API.";
    }

    super(message);

    Object.setPrototypeOf(this, new.target.prototype);
    this.name = GenericError.name;
  }

  static withMessage(message: string): GenericError {
    return new GenericError(message);
  }
}

export class NotFoundError extends Error {
  constructor(message?: string) {
    if (message === undefined) {
      message = "Not Found";
    }

    super(message);

    Object.setPrototypeOf(this, new.target.prototype);
    this.name = NotFoundError.name;
  }
}
