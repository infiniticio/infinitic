import Axios, { AxiosError } from "axios";
import { useApiMocks } from "@/utils/env";

export function isAxiosError(error: Error): error is AxiosError {
  return error && "isAxiosError" in error;
}

export const axiosClient = Axios.create({
  baseURL: useApiMocks ? undefined : "http://localhost:3010"
});
