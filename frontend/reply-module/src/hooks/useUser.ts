import axios from "axios";
import { useEffect } from "react";
import { useQuery, useQueryClient } from "react-query";
import { QUERY } from "../constants/api";
import { NO_ACCESS_TOKEN } from "../constants/errorName";
import { REACT_QUERY_KEY } from "../constants/reactQueryKey";
import { User } from "../types/user";
import { AlertError } from "../utils/Error";
import { request } from "../utils/request";
import { useToken } from "./useToken";

const getUser = async () => {
  try {
    const response = await request.get(QUERY.USER, { withCredentials: true });

    return response.data;
  } catch (error) {
    if (!axios.isAxiosError(error)) {
      throw new AlertError("알 수 없는 에러입니다.");
    }

    if (error.response?.data.code === 806 || error.response?.data.code === 801) {
      const newError = new Error("액세스 토큰이 존재하지 않습니다.");
      newError.name = NO_ACCESS_TOKEN;

      throw newError;
    }

    throw new AlertError("유저정보 조회에 실패하였습니다.\n잠시 후 다시 시도해주세요.");
  }
};

export const useUser = () => {
  const queryClient = useQueryClient();
  const { accessToken, deleteMutation, error: refreshError } = useToken(true);
  const {
    data: user,
    isLoading,
    error,
    refetch
  } = useQuery<User, Error>([REACT_QUERY_KEY.USER], getUser, {
    retry: false,
    enabled: !!accessToken
  });

  const logout = () => {
    deleteMutation.mutate();
    queryClient.setQueryData<User | undefined>(REACT_QUERY_KEY.USER, () => {
      return undefined;
    });
  };

  useEffect(() => {
    if (refreshError) {
      logout();
    }
  }, [refreshError]);

  return { user, isLoading, error, refetch, logout };
};
