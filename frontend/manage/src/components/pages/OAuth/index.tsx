import { QUERY } from "@/constants";
import { useToken } from "@/hooks";
import { request } from "@/utils/request";
import { useEffect } from "react";
import { useLocation, useParams } from "react-router";
import LoadingPage from "../LoadingPage";

const OAuth = () => {
  const location = useLocation();
  const { provider } = useParams<{ provider: string }>();
  const urlSearchParams = new URLSearchParams(location.search);
  const code = urlSearchParams.get("code");
  const { refetchAccessToken } = useToken();

  useEffect(() => {
    if (!code) return;

    const setAccessTokenAsync = async () => {
      try {
        await request.post(QUERY.LOGIN, {
          oauthProviderName: provider,
          authorizationCode: code
        });

        refetchAccessToken();
      } catch (error) {
        console.error(error);
      }
    };

    setAccessTokenAsync();
  }, [code]);

  return <LoadingPage />;
};

export default OAuth;